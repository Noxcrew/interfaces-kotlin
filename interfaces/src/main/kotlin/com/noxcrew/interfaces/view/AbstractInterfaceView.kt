package com.noxcrew.interfaces.view

import com.google.common.collect.HashMultimap
import com.noxcrew.interfaces.Constants.SCOPE
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
import com.noxcrew.interfaces.utilities.forEachInGrid
import com.noxcrew.interfaces.utilities.runSync
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The basis for the implementation of an interface view. */
public abstract class AbstractInterfaceView<I : InterfacesInventory, T : Interface<T, P>, P : Pane>(
    override val player: Player,
    /** The interface backing this view. */
    public val backing: T,
    private val parent: InterfaceView?
) : InterfaceView {

    public companion object {
        /** The amount of columns a chest inventory has. */
        public const val COLUMNS_IN_CHEST: Int = 9
    }

    private val logger = LoggerFactory.getLogger(AbstractInterfaceView::class.java)
    private val semaphore = Semaphore(1)
    private val queue = AtomicInteger(0)

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

    private val pendingTransforms = ConcurrentHashMap.newKeySet<AppliedTransform<P>>()
    private val debouncedTransforms = ConcurrentHashMap.newKeySet<AppliedTransform<P>>()

    private val panes = CollapsablePaneMap.create(backing.createPane())
    internal lateinit var pane: CompletedPane

    protected lateinit var currentInventory: I

    override val shouldStillBeOpened: Boolean
        get() = shouldBeOpened.get()

    override val isTreeOpened: Boolean
        get() = shouldStillBeOpened || children.keys.any { it.shouldStillBeOpened }

    /** Creates a new inventory GUI. */
    public abstract fun createInventory(): I

    /** Opens the inventory GUI for the viewer. */
    public abstract fun openInventory()

    /** Marks this menu as closed and processes it. */
    internal suspend fun markClosed(
        reason: InventoryCloseEvent.Reason = InventoryCloseEvent.Reason.UNKNOWN,
        changingView: Boolean = reason == InventoryCloseEvent.Reason.OPEN_NEW
    ) {
        // End a possible chat query with the listener
        InterfacesListeners.INSTANCE.abortQuery(player.uniqueId, this)

        // Ensure that the menu does not open
        openIfClosed.set(false)

        // Run a generic close handler if it's still opened
        if (shouldBeOpened.compareAndSet(true, false) && (!changingView || builder.callCloseHandlerOnViewSwitch)) {
            builder.closeHandlers[reason]?.invoke(reason, this)
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
                    child.close(reason, false)
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

    override suspend fun open() {
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
            renderAndOpen()
        }
    }

    override suspend fun close(reason: InventoryCloseEvent.Reason, changingView: Boolean) {
        markClosed(reason, changingView)

        // Ensure we always close on the main thread! Don't close if we are
        // changing views though.
        if (!changingView && isOpen()) {
            runSync {
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

    private suspend fun renderAndOpen() {
        // Don't update if closed
        if (!openIfClosed.get() && !isOpen()) return

        // If there is already queue of 2 renders we don't bother!
        if (queue.get() >= 2) return

        // Await to acquire a semaphore before starting the render
        queue.incrementAndGet()
        semaphore.acquire()
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
            semaphore.release()
            queue.decrementAndGet()
        }
    }

    private fun applyTransforms(transforms: Collection<AppliedTransform<P>>): Boolean {
        // Remove all these from the debounced transforms so we can try running
        // them again!
        debouncedTransforms -= transforms.toSet()

        // Check if the player is offline or the server stopping
        if (Bukkit.isStopping() || !player.isOnline) return false

        transforms.forEach { transform ->
            // If the transform is already pending we debounce it
            if (transform in pendingTransforms) {
                debouncedTransforms += transform
                return@forEach
            }

            // Indicate this transform is running which prevents the menu
            // from rendering until all transforms are done!
            pendingTransforms += transform

            SCOPE.launch {
                try {
                    // Don't run transforms for an offline player!
                    if (!Bukkit.isStopping() && player.isOnline) {
                        withTimeout(6.seconds) {
                            runTransformAndApplyToPanes(transform)
                        }
                    }
                } catch (exception: Exception) {
                    logger.error("Failed to run and apply transform: $transform", exception)
                } finally {
                    // Update that this transform has finished and check if
                    // we are ready to draw the screen finally!
                    pendingTransforms -= transform

                    if (transform in debouncedTransforms && applyTransforms(listOf(transform))) {
                        // Simply run the transform again here and do nothing else
                    } else {
                        // If all transforms are done we can finally draw and open the menu
                        if (pendingTransforms.isEmpty()) {
                            renderAndOpen()
                        }
                    }
                }
            }
        }

        // In the case that transforms was empty we might be able to open the menu already
        if (pendingTransforms.isEmpty()) {
            SCOPE.launch {
                renderAndOpen()
            }
        }
        return true
    }

    private suspend fun runTransformAndApplyToPanes(transform: AppliedTransform<P>) {
        val pane = backing.createPane()
        transform(pane, this@AbstractInterfaceView)
        val completedPane = pane.complete(player)

        // Access to the pane has to be shared through a semaphore
        semaphore.acquire()
        panes[transform.priority] = completedPane
        semaphore.release()
    }

    protected open fun drawPaneToInventory(drawNormalInventory: Boolean, drawPlayerInventory: Boolean) {
        // Determine all slots we need to clear if unused
        val leftovers = mutableListOf<Pair<Int, Int>>()
        forEachInGrid(backing.totalRows(), COLUMNS_IN_CHEST) { row, column ->
            leftovers += row to column
        }

        var madeChanges = false
        pane.forEach { row, column, element ->
            // We defer drawing of any elements in the player inventory itself
            // for later unless the inventory is already open.
            val isPlayerInventory = currentInventory.isPlayerInventory(row, column)
            if ((!drawNormalInventory && !isPlayerInventory) || (!drawPlayerInventory && isPlayerInventory)) return@forEach

            currentInventory.set(
                row,
                column,
                element.itemStack.apply { this?.let { builder.itemPostProcessor?.invoke(it) } }
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
                    item
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
            if (pane.getRawUnordered(point) != null) continue

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

        // Draw the contents of the inventory synchronously because
        // we don't want it to happen in between ticks and show
        // a half-finished inventory.
        runSync {
            // If the menu has since been requested to close we ignore all this
            if (!shouldBeOpened.get()) return@runSync

            // Determine if the inventory is currently open or being opened immediately,
            // otherwise we never draw to player inventories. This ensures lingering
            // updates on menus that have closed do not affect future menus that actually
            // ended up being opened.
            val isOpen = isOpen()
            drawPaneToInventory(drawNormalInventory = true, drawPlayerInventory = isOpen)
            callback(createdInventory)

            if ((openIfClosed.get() && !isOpen) || createdInventory) {
                openInventory()
                openIfClosed.set(false)
                firstPaint = false
            }
        }
    }

    override fun runChatQuery(timeout: Duration, onCancel: () -> Unit, onComplete: (Component) -> Unit) {
        InterfacesListeners.INSTANCE.startChatQuery(this, timeout, onCancel, onComplete)
    }
}
