package com.noxcrew.interfaces.view

import com.google.common.collect.HashMultimap
import com.noxcrew.interfaces.InterfacesConstants.SCOPE
import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.event.DrawPaneEvent
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.InterfaceBuilder
import com.noxcrew.interfaces.inventory.InterfacesInventory
import com.noxcrew.interfaces.pane.CompletedPane
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.pane.complete
import com.noxcrew.interfaces.properties.Trigger
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.utilities.CollapsablePaneMap
import com.noxcrew.interfaces.utilities.InterfacesCoroutineDetails
import com.noxcrew.interfaces.utilities.forEachInGrid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.slf4j.LoggerFactory
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The basis for the implementation of an interface view. */
public abstract class AbstractInterfaceView<I : InterfacesInventory, T : Interface<T, P>, P : Pane>(
    override val player: Player,
    /** The interface backing this view. */
    public val backing: T,
    private val parent: InterfaceView?,
) : InterfaceView {

    public companion object {
        /** The amount of columns a chest inventory has. */
        public const val COLUMNS_IN_CHEST: Int = 9
    }

    private val logger = LoggerFactory.getLogger(AbstractInterfaceView::class.java)
    private val paneMutex = Mutex()
    private val debouncedRender = AtomicBoolean(false)

    private val children = WeakHashMap<AbstractInterfaceView<*, *, *>, Unit>()

    /** The builder used by this interface. */
    public val builder: InterfaceBuilder<P, T>
        get() = backing.builder

    /** Added persistent items added when this interface was last closed. */
    public val addedItems: MutableMap<GridPoint, ItemStack> = mutableMapOf()

    /** Whether the view is being painted for the first time. */
    protected var firstPaint: Boolean = true

    /** Whether a click is being processed. */
    public var isProcessingClick: Boolean = false

    private val shouldBeOpened = AtomicBoolean(false)
    private val openIfClosed = AtomicBoolean(false)

    private val pendingTransforms = ConcurrentLinkedQueue<AppliedTransform<P>>()
    private var transformingJob: Job? = null
    private val transformMutex = Mutex()

    private val panes = CollapsablePaneMap.create(backing.createPane())
    private lateinit var pane: CompletedPane

    protected lateinit var currentInventory: I

    override val shouldStillBeOpened: Boolean
        get() = shouldBeOpened.get()

    override val isTreeOpened: Boolean
        get() = shouldStillBeOpened || children.keys.any { it.isTreeOpened }

    /** The pane of this view. */
    public val completedPane: CompletedPane?
        get() = if (::pane.isInitialized) pane else null

    /** Creates a new inventory GUI. */
    public abstract fun createInventory(): I

    /** Opens the inventory GUI for the viewer. */
    public abstract fun openInventory()

    /** Marks down that this menu should be re-opened. */
    internal fun markAsReopenable() {
        shouldBeOpened.set(true)
    }

    /**
     * Marks this menu as closed and processes it. This does not actually perform
     * closing the menu, this method only handles the closing.
     */
    internal fun markClosed(
        coroutineScope: CoroutineScope,
        reason: InventoryCloseEvent.Reason = InventoryCloseEvent.Reason.UNKNOWN,
        changingView: Boolean = reason == InventoryCloseEvent.Reason.OPEN_NEW,
    ) {
        if (!changingView) {
            // End a possible chat query with the listener (unless we're changing views)
            InterfacesListeners.INSTANCE.abortQuery(player.uniqueId, this)
        }

        // Ensure that the menu does not open
        openIfClosed.set(false)

        // Run a generic close handler if it's still opened and if the inventory was actually opened at any point
        if (shouldBeOpened.compareAndSet(true, false) &&
            (!changingView || builder.callCloseHandlerOnViewSwitch) &&
            ::currentInventory.isInitialized
        ) {
            builder.closeHandlers[reason]?.also {
                coroutineScope.launch {
                    it.invoke(reason, this@AbstractInterfaceView)
                }
            }
        }

        // Don't close children when changing views!
        if (!changingView) {
            // Close any children, this is a bit of a lossy system,
            // we don't particularly care if this happens nicely we
            // just want to make sure the ones that need closing get
            // closed. The hashmap is weak so children can get GC'd
            // properly.
            for ((child) in children) {
                if (child.shouldBeOpened.get()) {
                    child.close(coroutineScope, reason, false)
                }
            }
        }
    }

    private fun setup() {
        // Determine for each trigger what transforms it updates
        val triggers = HashMultimap.create<Trigger, AppliedTransform<P>>()
        for (transform in builder.transforms) {
            for (trigger in transform.triggers) {
                triggers.put(trigger, transform)
            }
        }

        // Add listeners to all triggers and update its transforms
        for ((trigger, transforms) in triggers.asMap()) {
            if (transforms.isEmpty()) continue
            trigger.addListener(this) {
                // Apply the transforms for the new ones
                applyTransforms(transforms)
            }
        }

        // Run a complete update which draws all transforms
        // and then opens the menu again
        redrawComplete()
    }

    override fun redrawComplete() {
        applyTransforms(builder.transforms)
    }

    override suspend fun reopen(): Boolean {
        if (!player.isConnected) return false
        if (!shouldBeOpened.get()) return false
        open()
        return true
    }

    override suspend fun open() {
        // Don't open an interface for an offline player
        if (!player.isConnected || !coroutineContext.isActive) return

        // Indicate that the menu should be opened after the next time rendering completes
        // and that it should be open right now
        openIfClosed.set(true)
        shouldBeOpened.set(true)

        // Indicate to the parent that this child exists
        if (parent is AbstractInterfaceView<*, *, *>) {
            parent.children[this] = Unit
        }

        // If this menu overlaps the player inventory we always
        // need to do a brand new first paint every time!
        if (firstPaint || this !is ChestInterfaceView) {
            firstPaint = true
            setup()
        } else {
            triggerRerender()
        }
    }

    override fun close(coroutineScope: CoroutineScope, reason: InventoryCloseEvent.Reason, changingView: Boolean) {
        markClosed(coroutineScope, reason, changingView)

        // Ensure we always close on the main thread! Don't close if we are
        // changing views though.
        if (!changingView && isOpen()) {
            InterfacesListeners.INSTANCE.runSync {
                if (!player.isConnected) return@runSync
                player.closeInventory()
            }
        }
    }

    override fun parent(): InterfaceView? = parent

    override suspend fun back() {
        if (parent == null) {
            close()
        } else {
            parent.open()
        }
    }

    private suspend fun triggerRerender() {
        // Don't update if closed
        if (!openIfClosed.get() && !isOpen()) return

        // If we're already rendering we queue up another render!
        if (paneMutex.isLocked) {
            debouncedRender.set(true)
            return
        }

        // Await to acquire the mutex before we start rendering
        paneMutex.lock()
        try {
            withTimeout(6.seconds) {
                pane = panes.collapse(backing.totalRows(), builder.fillMenuWithAir)
                renderToInventory { createdNewInventory ->
                    // send an update packet if necessary
                    if (!createdNewInventory && requiresPlayerUpdate()) {
                        player.updateInventory()
                    }
                }
            }
        } finally {
            paneMutex.unlock()
        }

        // If we queued up a debounced render we trigger another one!
        if (debouncedRender.compareAndSet(true, false)) {
            triggerRerender()
        }
    }

    private fun applyTransforms(transforms: Collection<AppliedTransform<P>>): Boolean {
        // Check if the player is offline or the server stopping
        if (Bukkit.isStopping() || !player.isOnline) return false

        // Ignore if the transforms are empty
        if (transforms.isEmpty()) {
            // If there are no transforms we still need to open it!
            SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "triggering re-render with no transforms")) {
                triggerRerender()
            }
            return true
        }

        // Queue up the transforms
        pendingTransforms.addAll(transforms)

        // Check if the job is already running
        SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "triggering re-render with transforms")) {
            try {
                transformMutex.lock()

                // Start the job if it's not running currently!
                if (transformingJob == null || transformingJob?.isCompleted == true) {
                    transformingJob = SCOPE.launch(
                        InterfacesCoroutineDetails(player.uniqueId, "running and applying a transform"),
                    ) {
                        // Go through all pending transforms one at a time until
                        // we're fully done with all of them. Other threads may
                        // add additional ones as we go through the queue.
                        while (pendingTransforms.isNotEmpty()) {
                            // Removes the first pending transform
                            val transform = pendingTransforms.remove()

                            // Don't run transforms for an offline player!
                            if (!Bukkit.isStopping() && player.isOnline) {
                                withTimeout(6.seconds) {
                                    runTransformAndApplyToPanes(transform)
                                }
                            }
                        }

                        // After we have finished running all transforms we render and open
                        // the menu before ending this job.
                        triggerRerender()
                    }
                }
            } finally {
                transformMutex.unlock()
            }
        }
        return true
    }

    private suspend fun runTransformAndApplyToPanes(transform: AppliedTransform<P>) {
        val pane = backing.createPane()
        transform(pane, this@AbstractInterfaceView)
        val completedPane = pane.complete(player)

        // Access to the pane has to be shared through a semaphore
        paneMutex.lock()
        try {
            panes[transform.priority] = completedPane
        } finally {
            paneMutex.unlock()
        }
    }

    protected open fun drawPaneToInventory(drawNormalInventory: Boolean, drawPlayerInventory: Boolean) {
        // Stop drawing if the player disconnected
        if (!player.isConnected) return

        // Determine all slots we need to clear if unused
        val leftovers = mutableListOf<Pair<Int, Int>>()
        forEachInGrid(backing.totalRows(), COLUMNS_IN_CHEST) { row, column ->
            leftovers += row to column
        }

        var madeChanges = false
        completedPane?.forEach { row, column, element ->
            // We defer drawing of any elements in the player inventory itself
            // for later unless the inventory is already open.
            val isPlayerInventory = currentInventory.isPlayerInventory(row, column)
            if ((!drawNormalInventory && !isPlayerInventory) || (!drawPlayerInventory && isPlayerInventory)) return@forEach

            currentInventory.set(
                row,
                column,
                element.itemStack.apply { this?.let { builder.itemPostProcessor?.invoke(it) } },
            )
            leftovers -= row to column
            madeChanges = true
        }

        // Apply the overlay of persistent items on top
        if (builder.persistAddedItems) {
            for ((point, item) in addedItems) {
                val row = point.x
                val column = point.y
                val isPlayerInventory = currentInventory.isPlayerInventory(row, column)
                if ((!drawNormalInventory && !isPlayerInventory) || (!drawPlayerInventory && isPlayerInventory)) continue

                currentInventory.set(
                    row,
                    column,
                    item,
                )
                leftovers -= row to column
                madeChanges = true
            }
        }

        // If we inherit existing items we don't clear here!
        if (!builder.inheritExistingItems) {
            // Empty any slots that are not otherwise edited
            for ((row, column) in leftovers) {
                val isPlayerInventory = currentInventory.isPlayerInventory(row, column)
                if ((!drawNormalInventory && !isPlayerInventory) || (!drawPlayerInventory && isPlayerInventory)) continue
                currentInventory.set(row, column, ItemStack(Material.AIR))
                madeChanges = true
            }
        }

        if (madeChanges) {
            Bukkit.getPluginManager().callEvent(DrawPaneEvent(player, this, drawNormalInventory, drawPlayerInventory))
        }
    }

    /** Saves any persistent items based on [inventory]. */
    public fun savePersistentItems(inventory: Inventory) {
        if (!builder.persistAddedItems) return

        addedItems.clear()
        val contents = inventory.contents
        for (index in contents.indices) {
            // Ignore empty slots
            val stack = contents[index] ?: continue
            if (stack.type == Material.AIR) continue

            // Find the slot that this item is in
            val point = GridPoint.fromBukkitChestSlot(index) ?: continue

            // Ignore any items that are in the pane itself
            if (completedPane?.getRawUnordered(point) != null) continue

            // Store this item
            addedItems[point] = stack
        }
    }

    override fun onOpen() {
        // Whenever we open the inventory we draw all elements in the player inventory
        // itself. We do this in this hook because it runs after InventoryCloseEvent so
        // it properly happens as the last possible action.
        drawPaneToInventory(drawNormalInventory = false, drawPlayerInventory = true)
    }

    protected open fun requiresNewInventory(): Boolean = firstPaint

    protected open fun requiresPlayerUpdate(): Boolean = false

    protected open suspend fun renderToInventory(callback: (Boolean) -> Unit) {
        // If the menu has since been requested to close we ignore all this
        if (!shouldBeOpened.get()) return

        // If a new inventory is required we create one
        // and mark that the current one is not to be used!
        val createdInventory = if (requiresNewInventory()) {
            currentInventory = createInventory()
            true
        } else {
            false
        }

        // Exit if the coroutine context is no longer active
        if (!coroutineContext.isActive) return

        // Draw the contents of the inventory synchronously because
        // we don't want it to happen in between ticks and show
        // a half-finished inventory.
        InterfacesListeners.INSTANCE.runSync {
            // If the menu has since been requested to close we ignore all this
            if (!shouldBeOpened.get()) return@runSync

            // Save persistent items if the view is currently opened
            if (player.openInventory.topInventory.getHolder(false) == this) {
                savePersistentItems(player.openInventory.topInventory)
            }

            // Determine if the inventory is currently open or being opened immediately,
            // otherwise we never draw to player inventories. This ensures lingering
            // updates on menus that have closed do not affect future menus that actually
            // ended up being opened.
            val isOpen = isOpen()
            drawPaneToInventory(drawNormalInventory = true, drawPlayerInventory = isOpen)
            callback(createdInventory)

            if (this is PlayerInterfaceView) {
                // If this is a player inventory we can't update the inventory without
                // opening it, so we trigger opening it properly.
                if (!isOpen && player.isConnected) openInventory()
            } else {
                if ((openIfClosed.get() && !isOpen) || createdInventory) {
                    InterfacesListeners.INSTANCE.viewBeingOpened = this
                    if (player.isConnected) openInventory()
                    if (InterfacesListeners.INSTANCE.viewBeingOpened == this) {
                        InterfacesListeners.INSTANCE.viewBeingOpened = null
                    }
                }
            }
            openIfClosed.set(false)
            firstPaint = false
        }
    }

    override fun runChatQuery(timeout: Duration, onCancel: suspend () -> Unit, onComplete: suspend (Component) -> Boolean) {
        InterfacesListeners.INSTANCE.startChatQuery(this, timeout, onCancel, onComplete)
    }
}
