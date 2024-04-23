package com.noxcrew.interfaces

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.noxcrew.interfaces.Constants.SCOPE
import com.noxcrew.interfaces.click.ClickContext
import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.click.CompletableClickHandler
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.pane.PlayerPane
import com.noxcrew.interfaces.utilities.runSync
import com.noxcrew.interfaces.view.AbstractInterfaceView
import com.noxcrew.interfaces.view.ChestInterfaceView
import com.noxcrew.interfaces.view.InterfaceView
import com.noxcrew.interfaces.view.PlayerInterfaceView
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
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
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory
import java.util.EnumSet
import java.util.UUID
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
        private val REOPEN_REASONS = EnumSet.of(
            Reason.PLAYER,
            Reason.UNKNOWN,
            Reason.PLUGIN
        )

        /** The possible valid slot range inside the player inventory. */
        private val PLAYER_INVENTORY_RANGE = 0..40

        /** The slot index used to indicate a click was outside the UI. */
        private const val OUTSIDE_CHEST_INDEX = -999
    }

    /** Stores data for a single chat query. */
    private data class ChatQuery(
        val view: InterfaceView,
        val onCancel: () -> Unit,
        val onComplete: (Component) -> Unit,
        val id: UUID
    )

    private val logger = LoggerFactory.getLogger(InterfacesListeners::class.java)

    private val spamPrevention: Cache<UUID, Unit> = Caffeine.newBuilder()
        .expireAfterWrite(200.toLong(), TimeUnit.MILLISECONDS)
        .build()

    /** A cache of all ongoing chat queries. */
    private val queries: Cache<UUID, ChatQuery> = Caffeine.newBuilder()
        .build()

    /** A cache of open player interface views, with weak values. */
    private val openPlayerInterfaceViews: Cache<UUID, PlayerInterfaceView> = Caffeine.newBuilder()
        .weakValues()
        .build()

    /** Returns the currently open interface for [playerId]. */
    public fun getOpenInterface(playerId: UUID): PlayerInterfaceView? {
        // Check if the menu is definitely still meant to be open
        val result = openPlayerInterfaceViews.getIfPresent(playerId) ?: return null
        if (result.shouldStillBeOpened) {
            return result
        }
        openPlayerInterfaceViews.invalidate(playerId)
        return null
    }

    /** Updates the currently open interface for [playerId] to [view]. */
    public fun setOpenInterface(playerId: UUID, view: PlayerInterfaceView?) {
        if (view == null) {
            openPlayerInterfaceViews.invalidate(playerId)
        } else {
            abortQuery(playerId, null)
            openPlayerInterfaceViews.put(playerId, view)
        }
    }

    @EventHandler
    public fun onOpen(event: InventoryOpenEvent) {
        val holder = event.inventory.holder
        val view = convertHolderToInterfaceView(holder) ?: return

        // Abort any previous query the player had
        abortQuery(event.player.uniqueId, null)
        view.onOpen()
    }

    @EventHandler
    public fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder
        val view = holder as? AbstractInterfaceView<*, *> ?: return
        val reason = event.reason

        SCOPE.launch {
            // Mark the current view as closed properly
            view.markClosed(reason)

            // Try to open back up a previous interface
            if (reason in REOPEN_REASONS && !event.player.isDead) {
                getOpenInterface(event.player.uniqueId)?.open()
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        val view = convertHolderToInterfaceView(holder) ?: return
        val clickedPoint = clickedPoint(event) ?: return
        handleClick(view, clickedPoint, event.click, event, event.hotbarButton)
    }

    @EventHandler
    public fun onPlayerQuit(event: PlayerQuitEvent) {
        abortQuery(event.player.uniqueId, null)
        setOpenInterface(event.player.uniqueId, null)
    }

    @EventHandler(priority = EventPriority.LOW)
    public fun onInteract(event: PlayerInteractEvent) {
        if (event.action == Action.PHYSICAL) return
        if (event.useItemInHand() == Event.Result.DENY) return

        val player = event.player
        val view = getOpenInterface(player.uniqueId) ?: return

        val clickedPoint = if (event.hand == EquipmentSlot.HAND) {
            GridPoint.at(3, player.inventory.heldItemSlot)
        } else {
            PlayerPane.OFF_HAND_SLOT
        }
        val click = convertAction(event.action, player.isSneaking)

        // Check if the action is prevented if this slot is not freely
        // movable
        if (!canFreelyMove(view, clickedPoint) &&
            event.action in view.backing.properties.preventedInteractions
        ) {
            event.isCancelled = true
            return
        }

        handleClick(view, clickedPoint, click, event, -1)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        val view = getOpenInterface(player.uniqueId) ?: return
        val slot = player.inventory.heldItemSlot
        val droppedSlot = GridPoint.at(3, slot)

        // Don't allow dropping items that cannot be freely edited
        if (!canFreelyMove(view, droppedSlot)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public fun onDeath(event: PlayerDeathEvent) {
        // Determine the holder of the top inventory being shown (can be open player inventory)
        val view = convertHolderToInterfaceView(event.player.openInventory.topInventory.holder) ?: return

        // Ignore chest inventories!
        if (view is ChestInterfaceView) return

        // Tally up all items that the player cannot modify and remove them from the drops
        for (index in PLAYER_INVENTORY_RANGE) {
            val stack = event.player.inventory.getItem(index) ?: continue
            val x = index / 9
            val adjustedX = PlayerPane.PANE_ORDERING.indexOf(x)
            val point = GridPoint(adjustedX, index % 9)
            if (!canFreelyMove(view, point)) {
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
        SCOPE.launch {
            getOpenInterface(event.player.uniqueId)?.open()
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public fun onChat(event: AsyncChatEvent) {
        val player = event.player

        // Determine if they have a pending query and end it
        val query = queries.getIfPresent(player.uniqueId) ?: return
        queries.invalidate(player.uniqueId)

        // Complete the query and re-open the view
        query.onComplete(event.message())
        SCOPE.launch {
            query.view.open()
        }

        // Prevent the message from sending
        event.isCancelled = true
    }

    /** Extracts the clicked point from an inventory click event. */
    private fun clickedPoint(event: InventoryClickEvent): GridPoint? {
        // not really sure why this special handling is required,
        // the ordered pane system should solve this but this is the only
        // place where it's become an issue.
        if (event.inventory.holder is Player) {
            val index = event.slot
            if (index !in PLAYER_INVENTORY_RANGE) return null

            val x = index / 9
            val adjustedX = PlayerPane.PANE_ORDERING.indexOf(x)
            return GridPoint(adjustedX, index % 9)
        }

        val index = event.rawSlot
        if (index == OUTSIDE_CHEST_INDEX) return null
        return GridPoint.at(index / 9, index % 9)
    }

    /**
     * Converts an inventory holder to an [AbstractInterfaceView] if possible. If the holder is a player
     * their currently open player interface is returned.
     */
    public fun convertHolderToInterfaceView(holder: InventoryHolder?): AbstractInterfaceView<*, *>? {
        if (holder == null) return null

        // If it's an abstract view use that one
        if (holder is AbstractInterfaceView<*, *>) return holder

        // If it's the player's own inventory use the held one
        if (holder is HumanEntity) return getOpenInterface(holder.uniqueId)

        return null
    }

    /** Returns whether [clickedPoint] in [view] can be freely moved. */
    private fun canFreelyMove(
        view: AbstractInterfaceView<*, *>,
        clickedPoint: GridPoint,
    ): Boolean = view.pane.getRaw(clickedPoint)?.clickHandler == null && !view.backing.properties.preventClickingEmptySlots

    /** Handles a [view] being clicked at [clickedPoint] through some [event]. */
    private fun handleClick(
        view: AbstractInterfaceView<*, *>,
        clickedPoint: GridPoint,
        click: ClickType,
        event: Cancellable,
        slot: Int
    ) {
        // Determine the type of click, if nothing was clicked we allow it
        val clickHandler = view.pane.getRaw(clickedPoint)?.clickHandler

        // Optionally cancel clicking on other slots
        if (clickHandler == null) {
            if (view.backing.properties.preventClickingEmptySlots) {
                event.isCancelled = true
            }
            return
        }

        // Automatically cancel if throttling or already processing
        if (view.isProcessingClick || shouldThrottle(view.player)) {
            event.isCancelled = true
            return
        }

        // Only allow one click to be processed at the same time
        view.isProcessingClick = true

        // Forward this click to all pre-processors
        val clickContext = ClickContext(view.player, view, click, slot)
        view.backing.properties.clickPreprocessors
            .forEach { handler -> ClickHandler.process(handler, clickContext) }

        // Run the click handler and deal with its result
        val completedClickHandler = clickHandler
            .run { CompletableClickHandler().apply { handle(clickContext) } }
            .onComplete { ex ->
                if (ex != null) {
                    logger.error("Failed to run click handler for ${view.player.name}", ex)
                }
                view.isProcessingClick = false
            }

        if (!completedClickHandler.completingLater) {
            completedClickHandler.complete()
        } else {
            // Automatically cancel the click handler after 6 seconds max!
            Bukkit.getScheduler().runTaskLaterAsynchronously(
                plugin,
                Runnable { completedClickHandler.cancel() },
                120
            )
        }

        // Update the cancellation state of the event
        if (completedClickHandler.cancelled) {
            event.isCancelled = true
        }
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
    private fun shouldThrottle(player: Player): Boolean =
        if (spamPrevention.getIfPresent(player.uniqueId) == null) {
            spamPrevention.put(player.uniqueId, Unit)
            false
        } else {
            true
        }

    /** Starts a new chat query on [view]. */
    public fun startChatQuery(view: InterfaceView, timeout: Duration, onCancel: () -> Unit, onComplete: (Component) -> Unit) {
        // Determine if the player has this inventory open
        if (!view.isOpen()) return

        // Store the current open inventory and remove it from the cache so it does
        // not interfere and we can have the player be itemless
        val playerId = view.player.uniqueId
        openPlayerInterfaceViews.invalidate(playerId)

        runSync {
            // Close the current inventory to open another to avoid close reasons
            view.player.closeInventory(Reason.OPEN_NEW)

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
                id
            )
        )

        // Set a timer for to automatically cancel this query to prevent players
        // from being stuck in a mode they don't understand for too long
        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                val queryThen = queries.getIfPresent(playerId) ?: return@Runnable
                if (queryThen.id != id) return@Runnable

                // Remove the query, run the cancel handler, and re-open the view
                queries.invalidate(playerId)
                onCancel()
                SCOPE.launch {
                    view.open()
                }
            },
            timeout.inWholeMilliseconds / 50
        )
    }

    /** Aborts an ongoing query for [playerId]. */
    public fun abortQuery(playerId: UUID, view: InterfaceView?) {
        val query = queries.getIfPresent(playerId) ?: return

        // Only end the specific view on request
        if (view != null && query.view != view) return
        queries.invalidate(playerId)

        // Run the cancellation handler
        query.onCancel()

        // If a view is given we are already in a markClosed call
        // and we can leave it here!
        if (view != null) return

        // Mark the view as properly closed
        SCOPE.launch {
            (query.view as AbstractInterfaceView<*, *>).markClosed(Reason.PLAYER)
        }
    }
}
