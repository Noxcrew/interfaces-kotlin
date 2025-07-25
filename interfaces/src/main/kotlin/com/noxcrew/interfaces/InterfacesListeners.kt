package com.noxcrew.interfaces

import com.destroystokyo.paper.MaterialSetTag
import com.destroystokyo.paper.MaterialTags
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.noxcrew.interfaces.InterfacesConstants.SCOPE
import com.noxcrew.interfaces.click.ClickContext
import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.click.CompletableClickHandler
import com.noxcrew.interfaces.exception.InterfacesExceptionContext
import com.noxcrew.interfaces.exception.InterfacesOperation
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.pane.PlayerPane
import com.noxcrew.interfaces.utilities.InterfacesCoroutineDetails
import com.noxcrew.interfaces.view.AbstractInterfaceView
import com.noxcrew.interfaces.view.ChestInterfaceView
import com.noxcrew.interfaces.view.InterfaceView
import com.noxcrew.interfaces.view.PlayerInterfaceView
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryCloseEvent.Reason
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory
import java.util.EnumSet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Listens to bukkit events and manages the current state of all interfaces accordingly.
 */
public class InterfacesListeners private constructor(private val plugin: Plugin) : Listener {

    public companion object {
        /** The current instance for interface listeners class. */
        public lateinit var INSTANCE: InterfacesListeners
            private set

        /** Installs interfaces into this plugin. */
        public fun install(plugin: Plugin) {
            require(!::INSTANCE.isInitialized) { "Already installed!" }
            INSTANCE = InterfacesListeners(plugin)
            Bukkit.getPluginManager().registerEvents(INSTANCE, plugin)
        }

        /** All valid closing reasons that should re-open the previously opened player inventory. */
        public val REOPEN_REASONS: Set<Reason> = EnumSet.of(
            Reason.PLAYER,
            Reason.UNKNOWN,
            Reason.PLUGIN,
            Reason.TELEPORT,
            Reason.CANT_USE,
            Reason.UNLOADED,
        )

        /** An incomplete set of blocks that have some interaction when clicked on. */
        private val CLICKABLE_BLOCKS: MaterialSetTag =
            MaterialSetTag(NamespacedKey("interfaces", "clickable-blocks"))
                .add(
                    MaterialTags.WOODEN_DOORS,
                    MaterialTags.WOODEN_TRAPDOORS,
                    MaterialTags.FENCE_GATES,
                    MaterialSetTag.BUTTONS,
                )
                // Add blocks with inventories
                .add(
                    Material.CHEST,
                    Material.ENDER_CHEST,
                    Material.TRAPPED_CHEST,
                    Material.BARREL,
                    Material.FURNACE,
                    Material.BLAST_FURNACE,
                    Material.SMOKER,
                    Material.CRAFTING_TABLE,
                    Material.LOOM,
                    Material.CARTOGRAPHY_TABLE,
                    Material.ENCHANTING_TABLE,
                    Material.SMITHING_TABLE,
                )
                .add(Material.LEVER)
                .add(Material.CAKE)
                // Add copper doors & trapdoors as they do not have their own tags
                .add(
                    Material.COPPER_DOOR,
                    Material.EXPOSED_COPPER_DOOR,
                    Material.WEATHERED_COPPER_DOOR,
                    Material.OXIDIZED_COPPER_DOOR,
                )
                .add(
                    Material.WAXED_COPPER_DOOR,
                    Material.WAXED_EXPOSED_COPPER_DOOR,
                    Material.WAXED_WEATHERED_COPPER_DOOR,
                    Material.WAXED_OXIDIZED_COPPER_DOOR,
                )
                .add(
                    Material.COPPER_TRAPDOOR,
                    Material.EXPOSED_COPPER_TRAPDOOR,
                    Material.WEATHERED_COPPER_TRAPDOOR,
                    Material.OXIDIZED_COPPER_TRAPDOOR,
                )
                .add(
                    Material.WAXED_COPPER_TRAPDOOR,
                    Material.WAXED_EXPOSED_COPPER_TRAPDOOR,
                    Material.WAXED_WEATHERED_COPPER_TRAPDOOR,
                    Material.WAXED_OXIDIZED_COPPER_TRAPDOOR,
                )
                // You can click signs to edit them
                .add(MaterialTags.SIGNS)
    }

    /** Stores data for a single chat query. */
    private data class ChatQuery(
        val view: InterfaceView,
        val onCancel: suspend () -> Unit,
        val onComplete: suspend (Component) -> Boolean,
        val id: UUID,
    )

    private val logger = LoggerFactory.getLogger(InterfacesListeners::class.java)

    private val spamPrevention: Cache<UUID, Unit> = Caffeine.newBuilder()
        .expireAfterWrite(200.toLong(), TimeUnit.MILLISECONDS)
        .build()

    private var dontReopen: Boolean = false

    /** The inventory currently opened by players, we track this internally because the [InventoryCloseEvent] has the wrong parameters. */
    private val openInventory = ConcurrentHashMap<HumanEntity, AbstractInterfaceView<*, *, *>>()

    /** A map of all ongoing chat queries. */
    private val queries = ConcurrentHashMap<UUID, ChatQuery>()

    /** A map of player interfaces that should be opened again in the future. */
    private val backgroundPlayerInterfaceViews = ConcurrentHashMap<UUID, PlayerInterfaceView>()

    /** A map of actively open player interfaces. */
    private val openPlayerInterfaceViews = ConcurrentHashMap<UUID, PlayerInterfaceView>()

    /** A map of interfaces being rendered for each player. */
    private val renderingPlayerInterfaceViews = ConcurrentHashMap<UUID, InterfaceView>()

    /** Runs [function] without reopening a new menu. */
    public fun withoutReopen(function: () -> Unit) {
        val oldValue = dontReopen
        dontReopen = true
        function()
        dontReopen = oldValue
    }

    /** Re-opens the current background interface of [player]. */
    public fun reopenInventory(player: Player) {
        // Don't re-open the background inventory when we're coming from the open event.
        if (dontReopen) return
        (getOpenPlayerInterface(player.uniqueId) ?: getBackgroundPlayerInterface(player.uniqueId))?.also {
            SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "reopening background interface")) {
                it.reopenIfIntended()
            }
        }
    }

    /**
     * Returns the background interface for [playerId]. This is the last
     * player interface that was opened, which should be re-opened once
     * we no longer have anything else showing.
     */
    public fun getBackgroundPlayerInterface(playerId: UUID): PlayerInterfaceView? {
        // Check if the menu is definitely still meant to be open
        val result = backgroundPlayerInterfaceViews[playerId] ?: return null
        if (result.shouldStillBeOpened) return result
        backgroundPlayerInterfaceViews -= playerId
        return null
    }

    /**
     * Returns the currently open player interface for [playerId].
     */
    public fun getOpenPlayerInterface(playerId: UUID): PlayerInterfaceView? {
        val result = openPlayerInterfaceViews[playerId] ?: return null
        if (result.shouldStillBeOpened) return result
        openPlayerInterfaceViews -= playerId
        return null
    }

    /** Sets the view currently being rendered for [playerId] to [view], returns `false` if this view is already being rendered. */
    public suspend fun setRenderView(playerId: UUID, view: InterfaceView): Boolean {
        // Close any view previously being rendered when opening a new one!
        // We remove from this map whenever rendering finishes to avoid any
        // unintended closes.
        if (renderingPlayerInterfaceViews[playerId] == view) return false
        renderingPlayerInterfaceViews.put(playerId, view)?.close(Reason.OPEN_NEW)
        return true
    }

    /** Marks that rendering has completed. */
    public fun completeRendering(playerId: UUID, view: InterfaceView) {
        renderingPlayerInterfaceViews.remove(playerId, view)
    }

    /** Sets the background view for [playerId] to [view]. */
    public fun setBackgroundView(playerId: UUID, view: PlayerInterfaceView?) {
        if (view == null) {
            backgroundPlayerInterfaceViews -= playerId
        } else {
            // For something to be the background view it has to be openable!
            view.markAsReopenable()
            backgroundPlayerInterfaceViews[playerId] = view
        }
    }

    /** Marks the given [view] as the opened player interface. */
    public fun setOpenView(playerId: UUID, view: PlayerInterfaceView) {
        completeRendering(playerId, view)
        backgroundPlayerInterfaceViews.remove(playerId, view)
        withoutReopen {
            if (openPlayerInterfaceViews[playerId] != view) {
                openPlayerInterfaceViews.put(playerId, view)?.close(SCOPE, Reason.OPEN_NEW)
            }
            val player = Bukkit.getPlayer(playerId) ?: return@withoutReopen
            if (openInventory[player] != view) {
                openInventory.put(player, view)?.close(SCOPE, Reason.OPEN_NEW)
            }
        }
    }

    /** Marks the given [view] of a player to be closed. */
    public fun markViewClosed(playerId: UUID, view: InterfaceView, abortQuery: Boolean = true) {
        if (abortQuery) {
            abortQuery(playerId, view)
        }
        renderingPlayerInterfaceViews.remove(playerId, view)
        backgroundPlayerInterfaceViews.remove(playerId, view)
        openPlayerInterfaceViews.remove(playerId, view)
    }

    /**
     * Saves any persistent items in the regular inventory of [player] if
     * we are keeping persistent items intact.
     */
    public fun saveInventoryContentsIfOpened(player: HumanEntity) {
        // Saves any persistent items stored in the main inventory whenever we are currently
        // showing a combined or player inventory before we draw the new one over-top
        val currentlyShown = convertHolderToInterfaceView(player.openInventory.topInventory.getHolder(false))
            ?: getBackgroundPlayerInterface(player.uniqueId)
        if (currentlyShown != null && currentlyShown !is ChestInterfaceView) {
            if (currentlyShown.builder.persistAddedItems) {
                currentlyShown.savePersistentItems(player.inventory)
            }
        }
    }

    @EventHandler
    public fun onOpen(event: InventoryOpenEvent) {
        // Save previous inventory contents before we open the new one
        saveInventoryContentsIfOpened(event.player)

        val holder = event.inventory.getHolder(false)
        val view = convertHolderToInterfaceView(holder) ?: return

        // Close the previous view first with open new as the reason, unless we
        // are currently opening this view!
        val openView = openInventory[event.player]
        if (openView != null && openView != view) {
            // If the previously opened view is a player interface, attempt to demote it to
            // the background interface and gracefully close it while marking it as re-openable
            // again after we're done.
            if (openView is PlayerInterfaceView && openPlayerInterfaceViews.remove(event.player.uniqueId, openView)) {
                val reopen = openView.shouldStillBeOpened
                withoutReopen {
                    openView.markClosed(SCOPE, Reason.OPEN_NEW)
                }
                if (reopen) {
                    backgroundPlayerInterfaceViews[event.player.uniqueId] = openView
                    openView.markAsReopenable()
                }
            } else {
                // Close whatever was previously opened completely
                withoutReopen {
                    openView.markClosed(SCOPE, Reason.OPEN_NEW)
                }
            }
        }

        // Set the new open inventory
        openInventory[event.player] = view

        // If there is an open view left, destroy it as something went wrong!
        withoutReopen {
            openPlayerInterfaceViews.remove(event.player.uniqueId)?.markClosed(SCOPE, Reason.OPEN_NEW)
        }

        // Abort any previous query the player had
        abortQuery(event.player.uniqueId, null)
        view.onOpen()
    }

    @EventHandler
    public fun onClose(event: InventoryCloseEvent) {
        val reason = event.reason

        // Save previous inventory contents before we open the new one (only if we have one open!)
        if (openInventory.containsKey(event.player)) {
            saveInventoryContentsIfOpened(event.player)
        }

        // When opening a new inventory we ignore the close event as it wrongly
        // reports the actual menu being closed! We rely entirely on the open event
        // and what we have previously stored as the open inventory.
        if (reason == Reason.OPEN_NEW) return

        // Ignore if the only open interface is a player inventory (you cannot close a player inventory)
        if (openInventory[event.player] is PlayerInterfaceView) return

        // Mark whatever inventory was open as closed!
        val opened = openInventory.remove(event.player)
        if (opened != null) {
            opened.markClosed(SCOPE, reason)
        } else if (reason in REOPEN_REASONS && !event.player.isDead) {
            // If the opened menu didn't trigger a re-open, do it manually!
            reopenInventory(event.player as Player)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.getHolder(false)
        val view = convertHolderToInterfaceView(holder) ?: return
        val clickedPoint = view.backing.mapper.toGridPoint(event.rawSlot) ?: return
        val isPlayerInventory = (event.clickedInventory ?: event.inventory).getHolder(false) is Player

        // Run base click handling
        if (handleClick(view, clickedPoint, event.click, event.hotbarButton, isPlayerInventory, false)) {
            event.isCancelled = true
        }

        // If the event is not cancelled we add extra prevention checks if any of the involved
        // slots are not allowed to be modified!
        if (!event.isCancelled) {
            if (view.backing.includesPlayerInventory) {
                // If you use a number key we check if the item you're swapping with is
                // protected.
                if (event.click == ClickType.NUMBER_KEY &&
                    !canFreelyMove(
                        view,
                        view.backing.relativizePlayerInventorySlot(GridPoint.at(3, event.hotbarButton)),
                        true,
                    )
                ) {
                    event.isCancelled = true
                    return
                }

                // If you try to swap with the off-hand we have to specifically check for that.
                if (event.click == ClickType.SWAP_OFFHAND &&
                    !canFreelyMove(
                        view,
                        view.backing.relativizePlayerInventorySlot(GridPoint.at(4, 4)),
                        true,
                    )
                ) {
                    event.isCancelled = true
                    return
                }
            }

            // Prevent double-clicking if it involves stealing any items
            val topInventory = event.view.topInventory
            val bottomInventory = event.view.bottomInventory
            if (event.click == ClickType.DOUBLE_CLICK) {
                val clickedItem = event.cursor
                val isInPlayerInventory = holder is Player

                // Don't check top inventory if we're in the player inventory!
                if (
                    (
                        !isInPlayerInventory &&
                            topInventory.withIndex().any { (index, it) ->
                                // Check if any item is being collected that cannot be moved!
                                it != null &&
                                    it.isSimilar(clickedItem) &&
                                    !canFreelyMove(
                                        view,
                                        requireNotNull(GridPoint.fromBukkitChestSlot(index)),
                                        false,
                                    )
                            }
                        ) ||
                    bottomInventory.withIndex().any { (index, it) ->
                        it != null &&
                            it.isSimilar(clickedItem) &&
                            // These slots are always in the player inventory and always need to be relativized!
                            !canFreelyMove(
                                view,
                                view.backing.relativizePlayerInventorySlot(
                                    requireNotNull(GridPoint.fromBukkitPlayerSlot(index)),
                                ),
                                true,
                            )
                    }
                ) {
                    event.isCancelled = true
                    return
                }
            }

            // If it's a shift click we have to detect what slot is being edited
            if (event.click.isShiftClick && event.clickedInventory != null) {
                val clickedInventory = event.clickedInventory!!
                var specialInventory = false
                val otherInventory =
                    if (clickedInventory == topInventory) {
                        bottomInventory
                    } else if (topInventory.type == InventoryType.CRAFTING) {
                        specialInventory = true
                        bottomInventory
                    } else {
                        topInventory
                    }

                // Ideally we predict which slot got shift clicked into! We start by finding any
                // stack that this item can be added onto, after that we find the first empty slot.
                val isMovingIntoPlayerInventory = otherInventory.getHolder(false) is Player
                val clickedItem = event.currentItem ?: ItemStack.empty()
                val firstEmptySlot = if (specialInventory) {
                    // If this is the player inventory you tab between the hotbar and the inventory's insides
                    val allowedIndices = if (clickedPoint.x != 3) {
                        // Moving into hotbar
                        otherInventory.contents.indices.filter { it < 9 }
                    } else {
                        // Moving into main inventory
                        otherInventory.contents.indices.filter { it in 9 until 36 }
                    }

                    allowedIndices.firstOrNull { index ->
                        val it = otherInventory.contents[index]
                        it != null && !it.isEmpty && it.isSimilar(clickedItem)
                    }.takeIf { it != -1 } ?: allowedIndices.firstOrNull { index ->
                        val it = otherInventory.contents[index]
                        it == null || it.isEmpty
                    } ?: -1
                } else {
                    otherInventory.indexOfFirst {
                        it != null && !it.isEmpty && it.isSimilar(clickedItem)
                    }.takeIf { it != -1 } ?: otherInventory.indexOfFirst { it == null || it.isEmpty }
                }

                if (firstEmptySlot != -1) {
                    val targetSlot = requireNotNull(
                        if (isMovingIntoPlayerInventory) {
                            GridPoint.fromBukkitPlayerSlot(firstEmptySlot)
                        } else {
                            GridPoint.fromBukkitChestSlot(firstEmptySlot)
                        },
                    )

                    if (!canFreelyMove(
                            view,
                            // If we are shift clicking into the player inventory
                            // we need to offset the target point into the inventory rows.
                            if (isMovingIntoPlayerInventory) {
                                view.backing.relativizePlayerInventorySlot(targetSlot)
                            } else {
                                targetSlot
                            },
                            isMovingIntoPlayerInventory,
                        )
                    ) {
                        event.isCancelled = true
                        return
                    }
                }
            }

            // It'd be nice if we had a way to redirect which slot gets shift clicked into, but this causes a giant mess
            // of plugin compatibility. The cleanest solution is for users to place invisible items in all taken slots and
            // to leave clickable slots open.
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public fun onDrag(event: InventoryDragEvent) {
        val holder = event.inventory.getHolder(false)
        val view = convertHolderToInterfaceView(holder) ?: return
        for (slot in event.rawSlots) {
            val clickedPoint = GridPoint.fromBukkitChestSlot(slot) ?: continue
            if (!canFreelyMove(view, clickedPoint, slot >= event.inventory.size)) {
                event.isCancelled = true
                return
            }
        }
    }

    /** Clean up everything for a player on disconnect. */
    @EventHandler(priority = EventPriority.HIGH)
    public fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerId = event.player.uniqueId
        openInventory.remove(event.player)?.markClosed(SCOPE, Reason.DISCONNECT)
        abortQuery(playerId, null)
        renderingPlayerInterfaceViews.remove(playerId)?.close(SCOPE, Reason.DISCONNECT)
        backgroundPlayerInterfaceViews.remove(playerId)?.close(SCOPE, Reason.DISCONNECT)
        openPlayerInterfaceViews.remove(playerId)?.close(SCOPE, Reason.DISCONNECT)
    }

    /** Returns whether [block] will trigger some interaction if clicked with [item]. */
    private fun hasInteraction(block: Block, item: ItemStack): Boolean = CLICKABLE_BLOCKS.isTagged(block)

    @EventHandler(priority = EventPriority.LOW)
    public fun onInteract(event: PlayerInteractEvent) {
        if (event.action == Action.PHYSICAL) return
        if (event.useItemInHand() == Event.Result.DENY) return

        val player = event.player
        val view = getOpenPlayerInterface(player.uniqueId) ?: return

        // If we are prioritizing block interactions we assure they are not happening first
        if (view.builder.prioritiseBlockInteractions) {
            // This is a bit messy because Bukkit doesn't cleanly give access to the block interactions. If you are
            // using this setting feel free to PR more logic into this method.
            if (event.clickedBlock != null && hasInteraction(event.clickedBlock!!, event.item ?: ItemStack.empty())) return
        }

        val clickedPoint = view.backing.relativizePlayerInventorySlot(
            if (event.hand == EquipmentSlot.HAND) {
                GridPoint.at(3, player.inventory.heldItemSlot)
            } else {
                PlayerPane.OFF_HAND_SLOT
            },
        )
        val click = convertAction(event.action, player.isSneaking)

        // Check if the action is prevented if this slot is not freely
        // movable
        if (!canFreelyMove(view, clickedPoint, true) &&
            event.action in view.builder.preventedInteractions
        ) {
            event.isCancelled = true
            return
        }

        if (handleClick(view, clickedPoint, click, -1, isPlayerInventory = true, interact = true)) {
            // Support modern behavior where we don't interfere with block interactions
            if (view.builder.onlyCancelItemInteraction) {
                event.setUseItemInHand(Event.Result.DENY)
            } else {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val view = getOpenPlayerInterface(player.uniqueId) ?: return
        val slot = player.inventory.heldItemSlot
        val droppedSlot = GridPoint.at(3, slot)

        // Don't allow dropping items that cannot be freely edited
        if (!canFreelyMove(view, view.backing.relativizePlayerInventorySlot(droppedSlot), true)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public fun onSwapHands(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val view = getOpenPlayerInterface(player.uniqueId) ?: return
        val slot = player.inventory.heldItemSlot
        val interactedSlot1 = GridPoint.at(3, slot)
        val interactedSlot2 = GridPoint.at(4, 4)

        // Don't allow swapping items that cannot be freely edited
        if (!canFreelyMove(view, view.backing.relativizePlayerInventorySlot(interactedSlot1), true) ||
            !canFreelyMove(view, view.backing.relativizePlayerInventorySlot(interactedSlot2), true)
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public fun onDeath(event: PlayerDeathEvent) {
        // Determine the holder of the top inventory being shown (can be open player inventory)
        val view = convertHolderToInterfaceView(event.player.openInventory.topInventory.getHolder(false)) ?: return

        // Ignore inventories that do not use the player inventory!
        if (!view.backing.includesPlayerInventory) return

        // Tally up all items that the player cannot modify and remove them from the drops
        for (index in GridPoint.PLAYER_INVENTORY_RANGE) {
            val stack = event.player.inventory.getItem(index) ?: continue
            val point = GridPoint.fromBukkitPlayerSlot(index) ?: continue
            if (!canFreelyMove(view, view.backing.relativizePlayerInventorySlot(point), true)) {
                var removed = false

                // Remove the first item in drops that is similar, drops will be a list
                // of exactly what was in the inventory, without merging any stacks. So
                // we do not need to do anything fancy to match the amounts.
                event.drops.removeIf {
                    if (!removed && it.isSimilar(stack)) {
                        removed = true
                        return@removeIf true
                    } else {
                        return@removeIf false
                    }
                }
            }
        }
    }

    @EventHandler
    public fun onRespawn(event: PlayerRespawnEvent) {
        reopenInventory(event.player)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public fun onChat(event: AsyncChatEvent) {
        val player = event.player

        // Determine if they have a pending query and end it
        val query = queries.remove(player.uniqueId) ?: return

        // Complete the query and re-open the view
        SCOPE.launch(InterfacesCoroutineDetails(event.player.uniqueId, "completing chat query")) {
            val abstractView = (query.view as AbstractInterfaceView<*, *, *>)
            abstractView.execute(
                InterfacesExceptionContext(
                    abstractView.player,
                    InterfacesOperation.CHAT_QUERY_COMPLETION,
                    abstractView,
                ),
            ) {
                if (query.onComplete(event.message())) {
                    query.view.reopenIfIntended()
                }
            }
        }

        // Prevent the message from sending
        event.isCancelled = true
    }

    /** Extracts the clicked point from an inventory click event. */
    private fun clickedPoint(view: AbstractInterfaceView<*, *, *>, event: InventoryClickEvent): GridPoint? {
        if (event.inventory.getHolder(false) is Player) {
            return GridPoint.fromBukkitPlayerSlot(event.slot)?.let { view.backing.relativizePlayerInventorySlot(it) }
        }
        return GridPoint.fromBukkitChestSlot(event.rawSlot)
    }

    /**
     * Converts an inventory holder to an [AbstractInterfaceView] if possible. If the holder is a player
     * their currently open player interface is returned.
     */
    public fun convertHolderToInterfaceView(holder: InventoryHolder?): AbstractInterfaceView<*, *, *>? {
        if (holder == null) return null

        // If it's an abstract view use that one
        if (holder is AbstractInterfaceView<*, *, *>) return holder

        // If it's the player's own inventory use the held one
        if (holder is HumanEntity) return getOpenPlayerInterface(holder.uniqueId)

        return null
    }

    /** Returns whether [clickedPoint] in [view] can be freely moved. */
    private fun canFreelyMove(view: AbstractInterfaceView<*, *, *>, clickedPoint: GridPoint, isPlayerInventory: Boolean): Boolean {
        // If we don't allow clicking empty slots we never allow freely moving
        if (view.builder.preventClickingEmptySlots &&
            !(view.builder.allowClickingOwnInventoryIfClickingEmptySlotsIsPrevented && isPlayerInventory)
        ) {
            return false
        }

        // If this inventory has no player inventory then the player inventory is always allowed to be edited
        if (!view.backing.includesPlayerInventory && isPlayerInventory) {
            return true
        }

        // If there is no item here we allow editing
        return view.completedPane?.getRaw(clickedPoint)?.itemStack == null
    }

    /** Handles a [view] being clicked at [clickedPoint] through some [event]. */
    private fun handleClick(
        view: AbstractInterfaceView<*, *, *>,
        clickedPoint: GridPoint,
        click: ClickType,
        slot: Int,
        isPlayerInventory: Boolean,
        interact: Boolean,
    ): Boolean {
        // Determine the type of click, if nothing was clicked we allow it
        val raw = view.completedPane?.getRaw(clickedPoint)

        // Determine if there is a click handler on this item
        val handler = raw?.clickHandler
            ?: return view.builder.preventClickingEmptySlots &&
                !(view.builder.allowClickingOwnInventoryIfClickingEmptySlotsIsPrevented && isPlayerInventory)

        // Prevent clicking on a decoration that is still loading!
        if (raw.pendingLazy?.requireDecorationToClick == true) {
            // Ensure decorations are running right now!
            view.ensureDecorating()
            return true
        }

        // Automatically cancel if throttling or already processing
        if (view.isProcessingClick || shouldThrottle(view.player)) {
            return true
        }

        // Only allow one click to be processed at the same time
        view.isProcessingClick = true

        // Forward this click to all pre-processors
        val clickContext = ClickContext(view.player, view, click, slot, interact)

        // Run the click handler and deal with its result
        val completedClickHandler = view.executeSync(
            InterfacesExceptionContext(
                view.player,
                InterfacesOperation.RUNNING_CLICK_HANDLER,
                view,
            ),
        ) {
            // Run any pre-processors
            view.builder.clickPreprocessors
                .forEach { handler -> ClickHandler.process(handler, clickContext) }

            // Run the click handler itself
            handler
                .run { CompletableClickHandler().apply { handle(clickContext) } }
                .onComplete { ex ->
                    if (ex != null) {
                        logger.error("Failed to run click handler for ${view.player.name}", ex)
                    }
                    view.isProcessingClick = false
                }
        } ?: return true

        if (!completedClickHandler.completingLater) {
            completedClickHandler.complete()
        } else {
            // Automatically cancel the click handler after 6 seconds max!
            Bukkit.getScheduler().runTaskLaterAsynchronously(
                plugin,
                Runnable { completedClickHandler.cancel() },
                120,
            )
        }

        // Update the cancellation state of the event
        if (completedClickHandler.cancelled) {
            return true
        }
        return false
    }

    /** Converts a bukkit [action] to a [ClickType]. */
    private fun convertAction(action: Action, sneaking: Boolean): ClickType {
        if (action.isRightClick) {
            if (sneaking) return ClickType.SHIFT_RIGHT
            return ClickType.RIGHT
        }
        if (action.isLeftClick) {
            if (sneaking) return ClickType.SHIFT_LEFT
            return ClickType.LEFT
        }
        return ClickType.UNKNOWN
    }

    /** Returns whether an inventory interaction from [player] should be throttled. */
    private fun shouldThrottle(player: Player): Boolean = if (spamPrevention.getIfPresent(player.uniqueId) == null) {
        spamPrevention.put(player.uniqueId, Unit)
        false
    } else {
        true
    }

    /** Starts a new chat query on [view]. */
    public fun startChatQuery(
        view: InterfaceView,
        timeout: Duration,
        onCancel: suspend () -> Unit,
        onComplete: suspend (Component) -> Boolean,
    ) {
        // Determine if the player has this inventory open
        if (!view.isOpen()) return

        // Store the current open inventory and remove it from the cache so it does
        // not interfere and we can have the player be itemless
        val playerId = view.player.uniqueId
        openPlayerInterfaceViews -= playerId

        runSync {
            // Close the current inventory to open another to avoid close reasons
            val reopen = view.shouldStillBeOpened
            withoutReopen {
                view.player.closeInventory(Reason.OPEN_NEW)
            }

            // Ensure the view is allowed to be opened again after we're done
            if (reopen) {
                (view as AbstractInterfaceView<*, *, *>).markAsReopenable()
            }

            // Clear the inventory
            view.player.inventory.clear()
        }

        // Set up the query
        val id = UUID.randomUUID()
        queries.put(
            playerId,
            ChatQuery(
                view,
                onCancel,
                onComplete,
                id,
            ),
        )

        // Set a timer for to automatically cancel this query to prevent players
        // from being stuck in a mode they don't understand for too long
        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                val queryThen = queries[playerId] ?: return@Runnable
                if (queryThen.id != id) return@Runnable

                // Remove the query, run the cancel handler, and re-open the view
                queries -= playerId
                SCOPE.launch(InterfacesCoroutineDetails(playerId, "cancelling chat query due to timeout")) {
                    val abstractView = (view as AbstractInterfaceView<*, *, *>)
                    abstractView.execute(
                        InterfacesExceptionContext(
                            abstractView.player,
                            InterfacesOperation.CHAT_QUERY_CANCELLATION,
                            abstractView,
                        ),
                    ) {
                        onCancel()
                    }
                    view.reopenIfIntended()
                }
            },
            timeout.inWholeMilliseconds / 50,
        )
    }

    /** Aborts an ongoing query for [playerId], without re-opening the original view. */
    public fun abortQuery(playerId: UUID, view: InterfaceView?) {
        // Only end the specific view on request
        if (view != null && queries[playerId]?.view != view) return
        val query = queries.remove(playerId) ?: return

        SCOPE.launch(InterfacesCoroutineDetails(playerId, "aborting chat query")) {
            // Run the cancellation handler
            val abstractView = (query.view as AbstractInterfaceView<*, *, *>)
            abstractView.execute(
                InterfacesExceptionContext(
                    abstractView.player,
                    InterfacesOperation.CHAT_QUERY_CANCELLATION,
                    abstractView,
                ),
            ) {
                query.onCancel()
            }

            // If a view is given we are already in a markClosed call
            // and we can leave it here!
            if (view != null) return@launch

            // Mark the view as properly closed
            abstractView.markClosed(SCOPE, Reason.PLAYER)
        }
    }

    /** Runs [function] on the main thread. */
    internal fun runSync(function: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            function()
            return
        }

        Bukkit.getScheduler().callSyncMethod(plugin, function)
    }
}
