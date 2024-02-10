package org.incendo.interfaces.next

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryCloseEvent.Reason
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.Plugin
import org.incendo.interfaces.next.Constants.SCOPE
import org.incendo.interfaces.next.click.ClickContext
import org.incendo.interfaces.next.click.ClickHandler
import org.incendo.interfaces.next.click.CompletableClickHandler
import org.incendo.interfaces.next.grid.GridPoint
import org.incendo.interfaces.next.pane.PlayerPane
import org.incendo.interfaces.next.view.AbstractInterfaceView
import org.incendo.interfaces.next.view.InterfaceView
import org.incendo.interfaces.next.view.PlayerInterfaceView
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.util.EnumSet
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

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

        private val VALID_REASON = EnumSet.of(
            Reason.PLAYER,
            Reason.UNKNOWN,
            Reason.PLUGIN
        )

        private val VALID_INTERACT = EnumSet.of(
            Action.LEFT_CLICK_AIR,
            Action.LEFT_CLICK_BLOCK,
            Action.RIGHT_CLICK_AIR,
            Action.RIGHT_CLICK_BLOCK
        )

        private val PLAYER_INVENTORY_RANGE = 0..40
        private const val OUTSIDE_CHEST_INDEX = -999
    }

    /** Stores data for a single chat query. */
    private data class ChatQuery(
        private val playerReference: WeakReference<Player>,
        private val openViewReference: WeakReference<PlayerInterfaceView>?,
        val view: InterfaceView,
        val onCancel: () -> Unit,
        val onComplete: (Component) -> Unit,
        val id: UUID
    ) {

        val player: Player?
            get() = playerReference.get()

        val openView: PlayerInterfaceView?
            get() = openViewReference?.get()
    }

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

        if (holder !is InterfaceView) {
            return
        }

        abortQuery(event.player.uniqueId, null)
        holder.onOpen()
    }

    @EventHandler
    public fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder
        val reason = event.reason

        if (holder !is InterfaceView) {
            return
        }

        SCOPE.launch {
            val view = convertHolderToInterfaceView(holder)
            if (view != null) {
                view.backing.closeHandlers[reason]?.invoke(reason, view)
            }

            if (reason !in VALID_REASON) return@launch
            getOpenInterface(event.player.uniqueId)?.open()
        }
    }

    @EventHandler
    public fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        val view = convertHolderToInterfaceView(holder) ?: return
        val clickedPoint = clickedPoint(event) ?: return
        handleClick(view, clickedPoint, event.click, event)
    }

    @EventHandler
    public fun onPlayerQuit(event: PlayerQuitEvent) {
        abortQuery(event.player.uniqueId, null)
        setOpenInterface(event.player.uniqueId, null)
    }

    @EventHandler
    public fun onInteract(event: PlayerInteractEvent) {
        if (event.action !in VALID_INTERACT) {
            return
        }
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        val player = event.player
        val view = getOpenInterface(player.uniqueId) as? AbstractInterfaceView<*, *> ?: return

        val slot = player.inventory.heldItemSlot
        val clickedPoint = GridPoint.at(3, slot)

        val click = convertAction(event.action, player.isSneaking)

        handleClick(view, clickedPoint, click, event)
    }

    @EventHandler
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
    }

    /** Extracts the clicked point from an inventory click event. */
    private fun clickedPoint(event: InventoryClickEvent): GridPoint? {
        // not really sure why this special handling is required,
        // the ordered pane system should solve this but this is the only
        // place where it's become an issue.
        if (event.inventory.holder is Player) {
            val index = event.slot

            if (index !in PLAYER_INVENTORY_RANGE) {
                return null
            }

            val x = index / 9
            val adjustedX = PlayerPane.PANE_ORDERING.indexOf(x)
            return GridPoint(adjustedX, index % 9)
        }

        val index = event.rawSlot

        if (index == OUTSIDE_CHEST_INDEX) {
            return null
        }

        return GridPoint.at(index / 9, index % 9)
    }

    /**
     * Converts an inventory holder to an [AbstractInterfaceView] if possible. If the holder is a player
     * their currently open player interface is returned.
     */
    public fun convertHolderToInterfaceView(holder: InventoryHolder?): AbstractInterfaceView<*, *>? {
        if (holder == null) {
            return null
        }

        // If it's an abstract view use that one
        if (holder is AbstractInterfaceView<*, *>) {
            return holder
        }

        // If it's the player's own inventory use the held one
        if (holder is HumanEntity) {
            return getOpenInterface(holder.uniqueId)
        }

        return null
    }

    /** Handles a [view] being clicked at [clickedPoint] through some [event]. */
    private fun handleClick(
        view: AbstractInterfaceView<*, *>,
        clickedPoint: GridPoint,
        click: ClickType,
        event: Cancellable
    ) {
        if (view.isProcessingClick || shouldThrottle(view.player)) {
            event.isCancelled = true
            return
        }

        view.isProcessingClick = true

        val clickContext = ClickContext(view.player, view, click)

        view.backing.clickPreprocessors
            .forEach { handler -> ClickHandler.process(handler, clickContext) }

        val clickHandler = view.pane.getRaw(clickedPoint)
            ?.clickHandler ?: ClickHandler.ALLOW

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

        event.isCancelled = completedClickHandler.cancelled
    }

    /** Converts a bukkit [action] to a [ClickType]. */
    private fun convertAction(action: Action, sneaking: Boolean): ClickType {
        if (action.isRightClick) {
            if (sneaking) {
                return ClickType.SHIFT_RIGHT
            }

            return ClickType.RIGHT
        }

        if (action.isLeftClick) {
            if (sneaking) {
                return ClickType.SHIFT_LEFT
            }

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
        val open = openPlayerInterfaceViews.getIfPresent(playerId)
        openPlayerInterfaceViews.invalidate(playerId)

        // Close the current inventory to open another to avoid close reasons
        view.player.closeInventory(Reason.OPEN_NEW)

        // Clear the inventory
        view.player.inventory.clear()

        // Set up the query
        val id = UUID.randomUUID()
        queries.put(
            playerId,
            ChatQuery(
                WeakReference(view.player),
                open?.let { WeakReference(it) },
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

        // Try to run the close handler on the view as it got closed now
        val reason = Reason.PLAYER
        (query.view as AbstractInterfaceView<*, *>).backing.closeHandlers[reason]?.also { handler ->
            SCOPE.launch {
                handler.invoke(reason, query.view)
            }
        }
    }
}
