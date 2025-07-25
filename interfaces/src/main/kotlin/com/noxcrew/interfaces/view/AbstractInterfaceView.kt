package com.noxcrew.interfaces.view

import com.google.common.collect.HashMultimap
import com.noxcrew.interfaces.InterfacesConstants.SCOPE
import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.InterfacesListeners.Companion.REOPEN_REASONS
import com.noxcrew.interfaces.element.CompletedElement
import com.noxcrew.interfaces.event.DrawPaneEvent
import com.noxcrew.interfaces.exception.InterfacesExceptionContext
import com.noxcrew.interfaces.exception.InterfacesExceptionHandler
import com.noxcrew.interfaces.exception.InterfacesExceptionResolution
import com.noxcrew.interfaces.exception.InterfacesOperation
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.InterfaceBuilder
import com.noxcrew.interfaces.inventory.InterfacesInventory
import com.noxcrew.interfaces.pane.CompletedPane
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.pane.PlayerPane
import com.noxcrew.interfaces.properties.LazyProperty
import com.noxcrew.interfaces.properties.StateProperty
import com.noxcrew.interfaces.properties.Trigger
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.transform.BlockingMode
import com.noxcrew.interfaces.transform.RefreshMode
import com.noxcrew.interfaces.utilities.CollapsablePaneMap
import com.noxcrew.interfaces.utilities.InterfacesCoroutineDetails
import com.noxcrew.interfaces.utilities.InterfacesProfiler
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

/**
 * The basis for the implementation of an interface view. A view holds a lot of information in memory, so
 * it should be weakly referenced anywhere that it is not held as the currently opened menu or as a parent
 * of a currently open menu. Any interface held in memory listens for changes, but no updates are processed
 * until the menu is re-opened.
 *
 * It is critical to ensure your views are properly garbage collected when they are discarded. Avoid keeping
 * references to views anywhere and make sure the open view stays referenced (done in InterfacesListeners) to
 * prevent them being collected before they are ready.
 *
 * For non-player inventories this object is used as the inventory holder so Bukkit retains the reference.
 */
public abstract class AbstractInterfaceView<I : InterfacesInventory, T : Interface<T, P>, P : Pane>(
    override val player: Player,
    /** The interface backing this view. */
    public val backing: T,
    birthParent: InterfaceView?,
) : InterfaceView, InterfacesExceptionHandler by backing.builder.exceptionHandler {

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

    /** Whether the title of the inventory should be re-painted. */
    protected var refreshTitle: Boolean = true

    /** Whether all properties should be fully reloaded. */
    protected var fullyReload: Boolean = false

    /** Whether all refresh type transforms should be reloaded. */
    protected var reloadRefresh: Boolean = false

    /** Whether a click is being processed. */
    public var isProcessingClick: Boolean = false

    /** The current parent of this view. */
    private var parent: InterfaceView? = birthParent
        set(value) {
            val oldValue = field
            field = value

            // Update the children maps of the parents!
            if (oldValue is AbstractInterfaceView<*, *, *>) {
                oldValue.children -= this
            }
            if (value is AbstractInterfaceView<*, *, *>) {
                value.children[this] = Unit
            }
        }

    private val shouldBeOpened = AtomicBoolean(false)
    private val openIfClosed = AtomicBoolean(false)
    private val queueAllTriggers = AtomicBoolean(false)

    private var supervisor = SupervisorJob()

    /** All transforms to process, split between blocking and non-blocking. */
    private val pendingTransforms = ConcurrentLinkedQueue<AppliedTransform<P>>()
    private var pendingNonBlockingTransforms = ConcurrentLinkedQueue<AppliedTransform<P>>()

    /** All transforms queued for a future open of the menu. */
    private var queuedTransforms = ConcurrentHashMap.newKeySet<AppliedTransform<P>>()

    private val propertiesJob = AtomicReference<Job>()
    private val propertiesMutex = Mutex()

    private val transformingJob = AtomicReference<Job>()
    private val transformMutex = Mutex()

    private val decoratingJob = AtomicReference<Job>()
    private val decorationMutex = Mutex()

    private val panes = CollapsablePaneMap.create()
    private lateinit var pane: CompletedPane

    /** All elements that need to be lazily updated. */
    private var lazyElements: ConcurrentLinkedQueue<CompletedElement> = ConcurrentLinkedQueue()

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

    init {
        // Log to the profiler
        InterfacesProfiler.log(this, "interface being constructed")

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
                // Inform the transform that the trigger has occurred
                transforms.forEach { it.handleChange(trigger) }
                applyTransforms(transforms, initial = false, renderIfEmpty = false)
            }
        }

        // Add this view to the parent
        if (birthParent is AbstractInterfaceView<*, *, *>) {
            birthParent.children[this] = Unit
        }
    }

    /** Creates a new inventory GUI. */
    public abstract fun createInventory(): I

    /** Opens the inventory GUI for the viewer. */
    public abstract fun openInventory()

    /** Marks down that this menu should be re-opened. */
    public fun markAsReopenable() {
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
        executeSync(
            InterfacesExceptionContext(
                player,
                InterfacesOperation.MARK_CLOSED,
                this,
            ),
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
                    // Run each close handler individually with an exception context and timeout
                    coroutineScope.launch {
                        execute(
                            InterfacesExceptionContext(
                                player,
                                InterfacesOperation.CLOSE_HANDLER,
                                this@AbstractInterfaceView,
                            ),
                        ) {
                            withTimeout(builder.defaultTimeout) {
                                it.invoke(reason, this@AbstractInterfaceView)
                            }
                        }
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

            // Cancel the supervisor job to cancel any rendering attempts, create a new supervisor for
            // any new tasks to run or if the menu is re-opened.
            supervisor.cancel()
            supervisor = SupervisorJob()
            propertiesJob.set(null)
            decoratingJob.set(null)
            transformingJob.set(null)

            // Queue up any unfinished transforms back up!
            queuedTransforms += pendingTransforms
            pendingTransforms.clear()
            val oldLazy = pendingNonBlockingTransforms
            pendingNonBlockingTransforms = ConcurrentLinkedQueue()
            queuedTransforms += oldLazy

            // Clear up all the state that might be half-finished!
            lazyElements.clear()
            queueAllTriggers.set(false)
            debouncedRender.set(false)

            // Test if a background menu should be opened
            if (reason in REOPEN_REASONS && !player.isDead) {
                InterfacesListeners.INSTANCE.reopenInventory(player)
            }
        }
    }

    override fun redrawComplete() {
        applyTransforms(
            builder.transforms.filter {
                it.refresh != RefreshMode.TRIGGER_ONLY &&
                    (it.refresh != RefreshMode.RELOAD || reloadRefresh)
            },
            initial = true, renderIfEmpty = true
        )
    }

    override suspend fun reopen(newParent: InterfaceView?, reload: Boolean): Boolean {
        // The player has since disconnected, close the menu properly!
        if (Bukkit.isStopping() || !player.isConnected) {
            if (shouldStillBeOpened) {
                markClosed(SCOPE, InventoryCloseEvent.Reason.DISCONNECT)
            }
            return false
        }

        // Don't open the menu if either it or its new parent is not meant
        // to be opened anymore!
        if (newParent?.isTreeOpened == false) {
            if (shouldStillBeOpened) {
                markClosed(SCOPE, InventoryCloseEvent.Reason.UNKNOWN)
            }
            return false
        }

        // Open the menu as normal.
        if (newParent != null) {
            parent = newParent
        }
        open(reload)
        return true
    }

    override suspend fun open(reload: Boolean) {
        // Don't open an interface for an offline player
        if (!player.isConnected || !coroutineContext.isActive) return

        // Mark this menu as the one being rendered for the player, we cancel any previous menus
        // being rendered when a new one is set.
        if (!InterfacesListeners.INSTANCE.setRenderView(player.uniqueId, this)) return

        // Indicate that the menu should be opened after the next time rendering completes
        // and that it should be open right now
        openIfClosed.set(true)
        shouldBeOpened.set(true)

        // Only fully re-evaluate all properties if this menu wants to, or if this menu is the first
        // in a chain (parent is not there or this is the first non-player interface)
        if (reload && (builder.alwaysReloadProperties || parent == null || parent is PlayerInterfaceView)) {
            fullyReload = true
        }

        // Trigger a refresh for RELOAD type transforms as long as reload is specified
        if (reload) {
            reloadRefresh = true
        }

        // If we want to redraw the title we use a new inventory always
        if (backing.builder.redrawTitleOnReopen) {
            refreshTitle = true
        }

        // Start by triggering all valid properties
        scheduleSingletonTask(propertiesMutex, propertiesJob, ::triggerProperties)
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
        val parent = parent()
        if (parent == null) {
            close()
        } else {
            parent.open(reload = false)
        }
    }

    /** Triggers all lazy and state properties to refresh, only runs on (re-)open. */
    private suspend fun triggerProperties() {
        try {
            queueAllTriggers.set(true)
            InterfacesProfiler.log(this, "starting to trigger properties")
            execute(
                InterfacesExceptionContext(
                    player,
                    InterfacesOperation.UPDATING_PROPERTIES,
                    this,
                ),
            ) {
                // Run the initialize method on any state properties to refresh them,
                // ensure we only refresh every property once even if we need it multiple
                // times!
                builder.transforms.flatMap { it.triggers }.filterIsInstance<StateProperty>().distinct().forEach {
                    withTimeout(builder.defaultTimeout) {
                        if (fullyReload) {
                            it.refresh(view = this@AbstractInterfaceView)
                        } else {
                            it.initialize(view = this@AbstractInterfaceView)
                        }
                    }
                }

                // Also re-evaluate all lazy properties!
                builder.transforms.flatMap { it.triggers }.filterIsInstance<LazyProperty<*>>().distinct().forEach {
                    withTimeout(builder.defaultTimeout) {
                        if (fullyReload) {
                            it.reevaluate(view = this@AbstractInterfaceView)
                        } else {
                            it.initialize(view = this@AbstractInterfaceView)
                        }
                    }
                }

                // Mark that we have done a reload
                fullyReload = false
            }
        } finally {
            queueAllTriggers.set(false)
        }

        // Either draw the entire interface or just re-render it
        InterfacesProfiler.log(this, "starting rendering")
        if (firstPaint) {
            queuedTransforms.clear()
            redrawComplete()
        } else {
            // Run any queued transforms while the menu was not shown if applicable, including any
            // transforms that always redraw
            val queued =
                queuedTransforms.toSet() +
                    builder.transforms.filter { it.refresh == RefreshMode.ALWAYS || (it.refresh == RefreshMode.RELOAD && reloadRefresh) }
                        .onEach {
                            // Reset any transforms that are not stale so they properly re-render!
                            it.reset()
                        }
            if (queued.isNotEmpty()) {
                queuedTransforms = ConcurrentHashMap.newKeySet()
                applyTransforms(queued, initial = false, renderIfEmpty = true)
            } else {
                triggerRerender()
            }
        }

        // Mark that we did a reload!
        reloadRefresh = false
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
        InterfacesProfiler.log(this, "starting re-rendering loop")
        paneMutex.lock()
        try {
            execute(
                InterfacesExceptionContext(
                    player,
                    InterfacesOperation.RENDER_INVENTORY,
                    this,
                ),
            ) {
                withTimeout(builder.defaultTimeout) {
                    // Collect the panes together and add air where necessary
                    pane = panes.collapse(backing.mapper, builder.allowClickingEmptySlots)

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
    private fun applyTransforms(transforms: Collection<AppliedTransform<P>>, initial: Boolean, renderIfEmpty: Boolean): Boolean {
        // Check if the player is offline or the server stopping
        if (Bukkit.isStopping() || !player.isConnected) return false

        // Ignore drawing transforms if the menu should be closed, but store them as
        // queued so we can execute them if we are asked to re-open!
        if (!shouldStillBeOpened || queueAllTriggers.get()) {
            queuedTransforms += transforms
            return false
        }

        // Ignore if the transforms are empty
        if (transforms.isEmpty()) {
            // If there are no transforms we still need to open it!
            if (renderIfEmpty) {
                SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "triggering re-render with no transforms") + supervisor) {
                    triggerRerender()
                }
            }
            return true
        }

        // Determine only the transforms that are not already pending!
        val alreadyQueuedTransforms = pendingTransforms.plus(pendingNonBlockingTransforms).toSet()
        val newTransforms = transforms.minus(alreadyQueuedTransforms)
        if (newTransforms.isEmpty()) return true
        val instantTransforms = if (initial) {
            newTransforms.filter { it.blocking != BlockingMode.NONE }
        } else {
            newTransforms.filter { it.blocking == BlockingMode.ALWAYS }
        }
        val lazyTransforms = if (initial) {
            newTransforms.filter { it.blocking == BlockingMode.NONE }
        } else {
            newTransforms.filter { it.blocking != BlockingMode.ALWAYS }
        }

        if (instantTransforms.isEmpty() || lazyTransforms.isEmpty()) {
            // If either category is empty we run everything immediately
            pendingTransforms.addAll(newTransforms)

            // If only the instant transforms are empty, we render first!
            if (instantTransforms.isEmpty() && renderIfEmpty) {
                SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "triggering re-render with no instant transforms") + supervisor) {
                    triggerRerender()
                }
            }
        } else {
            // Both categories must be filled so we separate them
            pendingTransforms.addAll(instantTransforms)
            pendingNonBlockingTransforms.addAll(lazyTransforms)
        }

        scheduleSingletonTask(transformMutex, transformingJob, ::processTransforms)
        return true
    }

    /** Processes all pending transforms. */
    private suspend fun processTransforms() {
        InterfacesProfiler.log(this, "starting transform processing")

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
                        this,
                    ),
                ) {
                    // Apply the transformation to the pane and build it within
                    // the allowed timeout!
                    val completedPane =
                        transform.completePane(backing.createPane(), player, builder, this@AbstractInterfaceView)

                    // Access to the pane has to be shared through a semaphore
                    // for extra safety, but we are just storing it!
                    // Don't apply a timeout to waiting in this queue!
                    paneMutex.lock()
                    try {
                        panes[transform.priority] = completedPane
                    } finally {
                        paneMutex.unlock()
                    }
                    InterfacesProfiler.log(this, "finished rendering transform")
                }
            }

            // After we have finished running all transforms we render and open
            // the menu before ending this job.
            triggerRerender()

            // After we complete a render we add any lazy transforms to the pending
            // list which may cause us to keep rendering even after the re-render
            val oldLazy = pendingNonBlockingTransforms
            pendingNonBlockingTransforms = ConcurrentLinkedQueue()
            pendingTransforms += oldLazy
        }
    }

    override fun ensureDecorating() {
        // Updates the current list of lazy elements to mirror all currently lazy elements
        val lazyElements = ConcurrentLinkedQueue<CompletedElement>()
        completedPane?.forEach { _, _, element ->
            element.pendingLazy?.also { lazyElements += element }
        }
        this.lazyElements = lazyElements

        // Start the task!
        scheduleSingletonTask(decorationMutex, decoratingJob, ::lazilyDecorateItems)
    }

    /** Starts a task to lazily decorate items. */
    private suspend fun lazilyDecorateItems() {
        while (true) {
            // Determine which element needs to be updated
            val element = lazyElements.poll() ?: break
            if (element.pendingLazy == null) continue
            execute(
                InterfacesExceptionContext(
                    player,
                    InterfacesOperation.DECORATING_ELEMENT,
                    this,
                ),
                onException = { _, resolution ->
                    when (resolution) {
                        InterfacesExceptionResolution.CLOSE -> return@execute

                        InterfacesExceptionResolution.RETRY -> {
                            // If the element was not finished we re-add it to the lazy list!
                            if (element.pendingLazy != null) {
                                lazyElements += element
                            }
                        }

                        InterfacesExceptionResolution.IGNORE -> {
                            // Don't attempt to run the lazy logic!
                            element.pendingLazy = null
                        }
                    }
                },
            ) {
                // With the timeout execute the lazy decoration task
                val item = element.itemStack?.clone() ?: ItemStack.empty()
                withTimeout(builder.defaultTimeout) {
                    element.pendingLazy?.decorate(player, item)
                }
                element.pendingLazy = null
                element.itemStack = if (item.isEmpty) null else item

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

    /** Schedules a new singleton task using [mutex] and [property]. */
    private fun scheduleSingletonTask(mutex: Mutex, property: AtomicReference<Job>, block: suspend () -> Unit) {
        // Try to start task if there is not already a job running
        if (property.get()?.isCompleted == false) return

        SCOPE.launch(InterfacesCoroutineDetails(player.uniqueId, "triggering singleton task") + supervisor) {
            try {
                // Use a mutex to ensure there are no two jobs!
                mutex.lock()

                // Start the job if it's not running currently!
                if (property.get()?.isCompleted == false) return@launch
                property.set(
                    SCOPE.launch(
                        InterfacesCoroutineDetails(player.uniqueId, "running singleton task") + supervisor,
                    ) { block() },
                )
            } finally {
                mutex.unlock()
            }
        }
    }

    protected open fun drawPaneToInventory(drawNormalInventory: Boolean, drawPlayerInventory: Boolean) {
        // Stop drawing if the player disconnected
        if (!player.isConnected) return

        // Determine all slots we need to clear if unused
        val leftovers = mutableListOf<Pair<Int, Int>>()
        backing.mapper.forEachInGrid { row, column ->
            leftovers += row to column
        }

        var madeChanges = false
        val lazyElements = ConcurrentLinkedQueue<CompletedElement>()
        completedPane?.forEach inner@{ row, column, element ->
            // Add all lazy elements to the list
            element.pendingLazy?.also { lazyElements += element }

            // We defer drawing of any elements in the player inventory itself
            // for later unless the inventory is already open.
            val isPlayerInventory = mapper.isPlayerInventory(row, column)
            if ((!drawNormalInventory && !isPlayerInventory) || (!drawPlayerInventory && isPlayerInventory)) return@inner

            currentInventory.set(
                row,
                column,
                element.itemStack?.takeUnless { it.isEmpty }?.also { builder.itemPostProcessor?.invoke(it) },
            )
            leftovers -= row to column
            madeChanges = true
        }

        // If any elements are not yet fully drawn start a task to do so!
        // Replace the previous list of lazy elements on the right group.
        // This prevents us from continuing to lazily decorate items that are
        // no longer drawn to the screen! (if players browse away from loading
        // pages)
        this.lazyElements = lazyElements
        if (lazyElements.isNotEmpty()) {
            scheduleSingletonTask(decorationMutex, decoratingJob, ::lazilyDecorateItems)
        }

        // Apply the overlay of persistent items on top
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

        // Empty any slots that are not otherwise edited
        for ((row, column) in leftovers) {
            val isPlayerInventory = mapper.isPlayerInventory(row, column)
            if ((!drawNormalInventory && !isPlayerInventory) || (!drawPlayerInventory && isPlayerInventory)) continue
            currentInventory.set(row, column, null)
            madeChanges = true
        }

        if (madeChanges) {
            Bukkit.getPluginManager().callEvent(DrawPaneEvent(player, this, drawNormalInventory, drawPlayerInventory))
        }
    }

    /** Saves any persistent items based on [inventory]. */
    public fun savePersistentItems(inventory: Inventory) {
        // We can only save items added to the player interface!
        if (this !is PlayerInterfaceView) return

        addedItems.clear()
        val contents = inventory.contents
        for (index in contents.indices) {
            // Ignore empty slots
            val stack = contents[index] ?: continue
            if (stack.type == Material.AIR) continue

            // Find the slot that this item is in
            val point = if (index == 40) {
                PlayerPane.OFF_HAND_SLOT
            } else if (index >= 36) {
                GridPoint(PlayerPane.EXTRA_ROW, 5 + (39 - index))
            } else if (index < 9) {
                GridPoint(3, index)
            } else {
                GridPoint.fromBukkitChestSlot(index - 9) ?: continue
            }

            // Ignore any items that are in the pane itself
            if (completedPane?.getRaw(point)?.itemStack != null) continue

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

    /** Hook for updating the title of the inventory. */
    protected open suspend fun updateTitle() {
    }

    protected open fun requiresNewInventory(): Boolean = firstPaint

    protected open fun requiresPlayerUpdate(): Boolean = false

    protected open suspend fun renderToInventory(callback: (Boolean) -> Unit) {
        // If the menu has since been requested to close we ignore all this
        if (!shouldBeOpened.get()) return

        // Try to update the title
        if (firstPaint || refreshTitle) {
            updateTitle()
        }

        // If a new inventory is required we create one
        // and mark that the current one is not to be used!
        val createdInventory = if (requiresNewInventory()) {
            currentInventory = createInventory()

            // Whenever we create a new inventory we have to re-mark this interface
            // as the one being rendered!
            InterfacesListeners.INSTANCE.setRenderView(player.uniqueId, this)
            true
        } else {
            false
        }

        // Exit if the coroutine context is no longer active
        if (!coroutineContext.isActive) return

        // Draw the contents of the inventory synchronously because
        // we don't want it to happen in between ticks and show
        // a half-finished inventory.
        InterfacesProfiler.log(this, "ready to render sync")
        InterfacesListeners.INSTANCE.runSync {
            executeSync(
                InterfacesExceptionContext(
                    player,
                    InterfacesOperation.SYNC_DRAW_INVENTORY,
                    this,
                ),
            ) {
                // If the menu has since been requested to close we ignore all this
                if (!shouldBeOpened.get()) return@executeSync

                // Save persistent items before we render
                if (builder.inheritExistingItems) {
                    savePersistentItems(player.inventory)
                }

                // Determine if the inventory is currently open or being opened immediately,
                // otherwise we never draw to player inventories. This ensures lingering
                // updates on menus that have closed do not affect future menus that actually
                // ended up being opened.
                val isOpen = isOpen()
                drawPaneToInventory(drawNormalInventory = true, drawPlayerInventory = isOpen)
                callback(createdInventory)
                InterfacesProfiler.log(this, "finished rendering to inventory")

                if (this is PlayerInterfaceView) {
                    // If this is a player inventory we can't update the inventory without
                    // opening it, so we trigger opening it properly.
                    if (openIfClosed.get() && !isOpen && player.isConnected) {
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
                refreshTitle = false
            }
        }
    }

    override fun runChatQuery(timeout: Duration, onCancel: suspend () -> Unit, onComplete: suspend (Component) -> Boolean) {
        InterfacesListeners.INSTANCE.startChatQuery(this, timeout, onCancel, onComplete)
    }
}
