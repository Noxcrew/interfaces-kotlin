package com.noxcrew.interfaces.interfaces

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.exception.InterfacesExceptionHandler
import com.noxcrew.interfaces.exception.StandardInterfacesExceptionHandler
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.transform.BlockingMode
import com.noxcrew.interfaces.transform.RefreshMode
import com.noxcrew.interfaces.utilities.InventorySegment
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Stores all shared properties of an interface. */
public open class InterfaceProperties<P : Pane> {

    public companion object {
        /** All reasons for a close handler. */
        public val ALL_REASONS: List<InventoryCloseEvent.Reason> =
            InventoryCloseEvent.Reason.entries

        /** All default reasons used for a new close handler. */
        public val DEFAULT_REASONS: List<InventoryCloseEvent.Reason> =
            ALL_REASONS.minus(InventoryCloseEvent.Reason.PLUGIN).minus(InventoryCloseEvent.Reason.OPEN_NEW)
    }

    private val _closeHandlers: MutableMap<InventoryCloseEvent.Reason, MutableList<CloseHandler>> = mutableMapOf()
    private val _clickPreprocessors: MutableCollection<ClickHandler> = mutableListOf()
    private val _preventedInteractions: MutableCollection<Action> = mutableListOf()
    private val preprocessors: Multimap<InventorySegment, InventoryProcessor> = HashMultimap.create()
    private val postprocessors: Multimap<InventorySegment, InventoryProcessor> = HashMultimap.create()

    // --- GENERAL ---
    /** The exception handler to use for this interface. */
    public var exceptionHandler: InterfacesExceptionHandler = StandardInterfacesExceptionHandler()

    /** The timeout to apply to all coroutines for this menu. */
    public var defaultTimeout: Duration = 5.seconds

    /** Enables debug logs for time spent on various actions. */
    public var debugRenderingTime: Boolean = false

    // --- CACHING / EFFICIENCY ---
    /** Whether to redraw the title when re-opening a menu. */
    public var redrawTitleOnReopen: Boolean = true

    /** If `true`, lazy and state properties are always re-evaluated instead of only initialized. */
    public var alwaysReloadProperties: Boolean = false

    /** The default refresh mode in this menu. */
    public var defaultRefreshMode: RefreshMode = RefreshMode.ALWAYS

    /** The default blocking mode for this menu. */
    public var defaultBlockMode: BlockingMode = BlockingMode.INITIAL

    // --- CLICK INTERACTIONS ---
    /** Whether to prioritise block interactions over item interactions when right-clicking. */
    public var prioritiseBlockInteractions: Boolean = false

    /** Whether interaction should only cancel the item effects and not the world effects. */
    public var onlyCancelItemInteraction: Boolean = false

    /** Whether to trigger click events on all empty slots. */
    public var allowClickingEmptySlots: Boolean = false

    /** Whether items can be moved into empty slots. */
    public var allowMovingEmptySlots: Boolean = false

    /** Whether clicking on empty slots should be fully cancelled. */
    public var preventClickingEmptySlots: Boolean = true

    // --- ITEM PERSISTENCE ---
    /**
     * Persists items added to this pane in a previous instance.
     * Particularly useful for player inventories, this allows the non-interface items
     * to function as normal inventory items and be normally added/removed.
     */
    public var persistAddedItems: Boolean = false

    /** Keeps items that were previously in the inventory before opening this. */
    public var inheritExistingItems: Boolean = false

    // --- CUSTOM HANDLER ---
    /** All close handlers on this interface mapped by closing reason. */
    public val closeHandlers: Map<InventoryCloseEvent.Reason, List<CloseHandler>>
        get() = _closeHandlers

    /** A collection of click handlers that will be run before each click without blocking. */
    public val clickPreprocessors: Collection<ClickHandler>
        get() = _clickPreprocessors

    /** All interactions that will be ignored on this view and cancelled on pane items without calling the handler. */
    public val preventedInteractions: Collection<Action>
        get() = _preventedInteractions

    /** A post-processor applied to all items placed in the inventory. */
    public var itemPostProcessor: ((ItemStack) -> Unit)? = {}

    init {
        useSimpleDefaults()
    }

    /** Gives all items in modifiable slots back to players when closed, unless they were already present. */
    public fun returnPlacedIntoInventoryOnClose() {
        addUnconditionalCloseHandler { _, view ->
            view.completedPane?.forEach { row, column, element ->
                if (!element.isSlotModifiable || element.itemStack != null) return@forEach
                val item = view.inventory?.get(row, column) ?: return@forEach
                view.player.inventory.addItem(item)
            }
        }
    }

    /** Returns all registered pre-processors for [segment]. */
    public fun getPreprocessors(segment: InventorySegment): Collection<InventoryProcessor> = preprocessors[segment]

    /** Returns all registered post-processors for [segment]. */
    public fun getPostprocessors(segment: InventorySegment): Collection<InventoryProcessor> = postprocessors[segment]

    /** Sets all values to their simple defaults. This is the default type! */
    public fun useSimpleDefaults() {
        redrawTitleOnReopen = true
        alwaysReloadProperties = true
        defaultRefreshMode = RefreshMode.ALWAYS
        defaultBlockMode = BlockingMode.ALWAYS
    }

    /** Sets all values to support efficient caching of components and minimize automatic refreshes. */
    public fun useCachingDefaults() {
        redrawTitleOnReopen = false
        alwaysReloadProperties = false
        defaultRefreshMode = RefreshMode.INITIAL
        defaultBlockMode = BlockingMode.INITIAL
    }

    /** Adds a new close handler [closeHandler] that triggers whenever the inventory is closed for any reason. */
    public fun addUnconditionalCloseHandler(closeHandler: CloseHandler): Unit = addCloseHandler(ALL_REASONS, closeHandler)

    /** Adds a new close handler [closeHandler] that triggers whenever the inventory is closed for any of the given [reasons]. */
    public fun addCloseHandler(reasons: Collection<InventoryCloseEvent.Reason> = DEFAULT_REASONS, closeHandler: CloseHandler) {
        reasons.forEach {
            _closeHandlers.computeIfAbsent(it) { mutableListOf() } += closeHandler
        }
    }

    /** Adds a new pre-processor to this menu which will run [handler] before every click without blocking. */
    public fun addPreprocessor(handler: ClickHandler) {
        _clickPreprocessors += handler
    }

    /** Adds [action] to be cancelled without triggering any click handlers on valid items in this pane. */
    public fun addPreventedAction(action: Action) {
        _preventedInteractions += action
    }

    /** Adds a new [preprocessor] to be called before [segment] is drawn to. */
    public fun withPreprocessor(segment: InventorySegment, preprocessor: InventoryProcessor) {
        preprocessors.put(segment, preprocessor)
    }

    /** Adds a new [postprocessor] to be called after [segment] was drawn. */
    public fun withPostprocessor(segment: InventorySegment, postprocessor: InventoryProcessor) {
        postprocessors.put(segment, postprocessor)
    }
}
