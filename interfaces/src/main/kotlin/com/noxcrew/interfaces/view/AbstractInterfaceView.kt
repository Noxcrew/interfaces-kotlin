package com.noxcrew.interfaces.view

import com.google.common.collect.HashMultimap
import com.noxcrew.interfaces.InterfacesConstants.SCOPE
import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.InterfacesListeners.Companion.REOPEN_REASONS
import com.noxcrew.interfaces.element.CompletedElement
import com.noxcrew.interfaces.event.DrawPaneEvent
import com.noxcrew.interfaces.exception.InterfacesExceptionContext
import com.noxcrew.interfaces.exception.InterfacesOperation
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
import kotlinx.coroutines.SupervisorJob
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

    private val mapper = backing.mapper

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

    private val supervisor = SupervisorJob()

    private val pendingTransforms = ConcurrentLinkedQueue<AppliedTransform<P>>()
    private var lazyPending = ConcurrentLinkedQueue<AppliedTransform<P>>()

    private var transformingJob: Job? = null
    private val transformMutex = Mutex()

    private var decoratingJob: Job? = null
    private val decorationMutex = Mutex()

    private val panes = CollapsablePaneMap.create(backing.createPane())
    private lateinit var pane: CompletedPane

    private var normalInventoryLazy: ConcurrentLinkedQueue<CompletedElement> = ConcurrentLinkedQueue()
    private var playerInventoryLazy: ConcurrentLinkedQueue<CompletedElement> = ConcurrentLinkedQueue()

    protected lateinit var currentInventory: I

    override val shouldStillBeOpened: Boolean
        get() = shouldBeOpened.get()

    override val isTreeOpened: Boolean
        get() = shouldStillBeOpened || children.keys.any { it.isTreeOpened }

    /** Whether this menu type overlaps the player inventory. */
    public open val overlapsPlayerInventory: Boolean
        get() = false

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
        changingView: Boolean = reason ==
            InventoryCloseEvent.Reason.OPEN_NEW,
    ) {
        // Mark this view as closed with the listener
        InterfacesListeners.INSTANCE.markViewClosed(player.uniqueId, this, abortQuery = !changingView)

        // Ensure that the menu does not open
        openIfClosed.set(false)

        // Run a generic close handler if it's still opened and if the inventory was actually opened at any point
        if (shouldBeOpened.compareAndSet(true, false) &&
            (!changingView || builder.callCloseHandlerOnViewSwitch) &&
            ::currentInventory.isInitialized
        ) {
            builder.closeHandlers[reason]?.forEach {
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

        // Cancel the supervisor job to cancel any rendering attempts
        supervisor.cancel()

        // Test if a background menu should be opened
        val backgroundInterface = InterfacesListeners.INSTANCE.getBackgroundPlayerInterface(player.uniqueId)
        val shouldReopen = reason in REOPEN_REASONS && !player.isDead && backgroundInterface != null
        if (shouldReopen) {
            SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "reopening background interface")) {
                backgroundInterface?.reopen()
            }
        }
    }

    /** Registers weak listeners on all transforms of this menu to re-render this menu. */
    private fun initialDraw() {
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
        // This menu was already closed, in case this menu is somehow lingering we'll
        // tell the listener again we already closed!
        if (!shouldBeOpened.get()) {
            InterfacesListeners.INSTANCE.markViewClosed(player.uniqueId, this)
            return false
        }

        // The player has since disconnected, close the menu properly!
        if (!player.isConnected) {
            markClosed(SCOPE, InventoryCloseEvent.Reason.DISCONNECT)
            return false
        }

        // Open the menu as normal.
        open()
        return true
    }

    override suspend fun open() {
        // Don't open an interface for an offline player
        if (!player.isConnected || !coroutineContext.isActive) return

        // Mark this menu as the one being rendered for the player, we cancel any previous menus
        // being rendered when a new one is set.
        if (!InterfacesListeners.INSTANCE.setRenderView(player.uniqueId, this)) return

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
        if (overlapsPlayerInventory) {
            firstPaint = true
        }

        // Perform the opening with a retry loop
        if (firstPaint) {
            initialDraw()
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

    /** Triggers a re-render of the inventory based on all currently completed panes. */
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
            execute(
                InterfacesExceptionContext(
                    player,
                    InterfacesOperation.RENDER_INVENTORY,
                ),
            ) {
                withTimeout(builder.defaultTimeout) {
                    // Collect the panes together and add air where necessary
                    pane = panes.collapse(backing.totalRows(), builder.fillMenuWithAir)

                    // Render the completed panes
                    renderToInventory { createdNewInventory ->
                        // send an update packet if necessary
                        if (!createdNewInventory && requiresPlayerUpdate()) {
                            player.updateInventory()
                        }
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

    /** Applies the given [transforms] to the interface. */
    private fun applyTransforms(transforms: Collection<AppliedTransform<P>>): Boolean {
        // Check if the player is offline or the server stopping
        if (Bukkit.isStopping() || !player.isConnected) return false

        // Ignore if the transforms are empty
        if (transforms.isEmpty()) {
            // If there are no transforms we still need to open it!
            SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "triggering re-render with no transforms") + supervisor) {
                triggerRerender()
            }
            return true
        }

        val instantTransforms = transforms.filter { it.blocking }
        val lazyTransforms = transforms.filterNot { it.blocking }

        if (instantTransforms.isEmpty() || lazyTransforms.isEmpty()) {
            // If either category is empty we run everything immediately
            pendingTransforms.addAll(transforms)
        } else {
            // Both categories must be filled so we separate them
            pendingTransforms.addAll(instantTransforms)
            lazyPending.addAll(lazyTransforms)
        }

        // Ignore if we know there is already a job as it will go through all pending transforms
        if (transformingJob != null && transformingJob?.isCompleted == false) return true

        // Check if the job is already running
        SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "triggering re-render with transforms") + supervisor) {
            try {
                transformMutex.lock()

                // Start the job if it's not running currently!
                if (transformingJob == null || transformingJob?.isCompleted == true) {
                    transformingJob = SCOPE.launch(
                        InterfacesCoroutineDetails(player.uniqueId, "running and applying a transform") + supervisor,
                    ) {
                        while (pendingTransforms.isNotEmpty()) {
                            // Go through all pending transforms one at a time until
                            // we're fully done with all of them. Other threads may
                            // add additional ones as we go through the queue.
                            while (pendingTransforms.isNotEmpty()) {
                                // Removes the first pending transform
                                val transform = pendingTransforms.remove()

                                // Handle executions properly for rendering transforms
                                execute(
                                    InterfacesExceptionContext(
                                        player,
                                        InterfacesOperation.APPLY_TRANSFORM,
                                    ),
                                ) {
                                    // Apply the transformation to the pane and build it within
                                    // the allowed timeout!
                                    val completedPane = withTimeout(builder.defaultTimeout) {
                                        val pane = backing.createPane()
                                        transform(pane, this@AbstractInterfaceView)
                                        pane.complete(player)
                                    }

                                    // Access to the pane has to be shared through a semaphore
                                    // for extra safety, but we are just storing it!
                                    // Don't apply a timeout to waiting in this queue!
                                    paneMutex.lock()
                                    try {
                                        panes[transform.priority] = completedPane
                                    } finally {
                                        paneMutex.unlock()
                                    }
                                }
                            }

                            // After we have finished running all transforms we render and open
                            // the menu before ending this job.
                            triggerRerender()

                            // After we complete a render we add any lazy transforms to the pending
                            // list which may cause us to keep rendering even after the re-render
                            val oldLazy = lazyPending
                            lazyPending = ConcurrentLinkedQueue<AppliedTransform<P>>()
                            pendingTransforms += oldLazy
                        }
                    }
                }
            } finally {
                transformMutex.unlock()
            }
        }
        return true
    }

    /** Starts a task to lazily decorate items. */
    private fun lazilyDecorateItems() {
        // Try to start decorating if there is not already a job running
        if (decoratingJob != null && decoratingJob?.isCompleted == false) return

        SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "triggering lazy draw") + supervisor) {
            try {
                // Use a mutex to ensure there are no two jobs!
                decorationMutex.lock()

                // Start the job if it's not running currently!
                if (decoratingJob == null || decoratingJob?.isCompleted == true) {
                    decoratingJob = SCOPE.launch(
                        InterfacesCoroutineDetails(player.uniqueId, "lazily decorating items") + supervisor,
                    ) {
                        while (true) {
                            // Determine which element needs to be updated
                            val element = normalInventoryLazy.poll() ?: playerInventoryLazy.poll() ?: break

                            execute(
                                InterfacesExceptionContext(
                                    player,
                                    InterfacesOperation.DECORATING_ELEMENT,
                                ),
                            ) {
                                // With the timeout execute the lazy decoration task
                                withTimeout(builder.defaultTimeout) {
                                    val item = element.itemStack?.clone() ?: ItemStack.empty()
                                    element.pendingLazy?.decorate(player, item)
                                    element.pendingLazy = null
                                    element.itemStack = if (item.isEmpty) null else item
                                }

                                // If we're already rendering we simply queue up the re-render but
                                // continue in this coroutine so we can hopefully get multiple
                                // elements decorated before to debounce is done.
                                if (paneMutex.isLocked) {
                                    debouncedRender.set(true)
                                    return@execute
                                }

                                // Trigger a re-rendering of the menu after each
                                // individual item stack has finished rendering!
                                SCOPE.launch(
                                    InterfacesCoroutineDetails(
                                        player.uniqueId,
                                        "triggering re-render after lazy draw",
                                    ) + supervisor,
                                ) {
                                    triggerRerender()
                                }
                            }
                        }
                    }
                }
            } finally {
                decorationMutex.unlock()
            }
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
        val lazyElements = ConcurrentLinkedQueue<CompletedElement>()
        completedPane?.forEach { row, column, element ->
            // We defer drawing of any elements in the player inventory itself
            // for later unless the inventory is already open.
            val isPlayerInventory = mapper.isPlayerInventory(row, column)
            if ((!drawNormalInventory && !isPlayerInventory) || (!drawPlayerInventory && isPlayerInventory)) return@forEach

            currentInventory.set(
                row,
                column,
                element.itemStack.apply { this?.let { builder.itemPostProcessor?.invoke(it) } },
            )
            element.pendingLazy?.also { lazyElements += element }
            leftovers -= row to column
            madeChanges = true
        }

        // If any elements are not yet fully drawn start a task to do so!
        // Replace the previous list of lazy elements on the right group.
        // This prevents us from continuing to lazily decorate items that are
        // no longer drawn to the screen! (if players browse away from loading
        // pages)
        if (drawNormalInventory) {
            normalInventoryLazy = lazyElements
            if (drawPlayerInventory) {
                playerInventoryLazy = ConcurrentLinkedQueue()
            }
        } else if (drawPlayerInventory) {
            playerInventoryLazy = lazyElements
        }
        if (lazyElements.isNotEmpty()) {
            lazilyDecorateItems()
        }

        // Apply the overlay of persistent items on top
        if (builder.persistAddedItems) {
            for ((point, item) in addedItems) {
                val row = point.x
                val column = point.y
                val isPlayerInventory = mapper.isPlayerInventory(row, column)
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
                val isPlayerInventory = mapper.isPlayerInventory(row, column)
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
            if (completedPane?.getRaw(point) != null) continue

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
                if (!isOpen && player.isConnected) {
                    openInventory()
                }
            } else {
                if ((openIfClosed.get() && !isOpen) || createdInventory) {
                    if (player.isConnected) {
                        openInventory()
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

    /** Executes [function], reporting any errors to the [InterfacesExceptionHandler] being used. */
    public suspend fun <T> execute(context: InterfacesExceptionContext, function: suspend () -> T): T? =
        builder.exceptionHandler.execute(context.copy(view = this), function)
}
