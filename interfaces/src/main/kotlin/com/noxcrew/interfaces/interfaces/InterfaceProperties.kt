package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.exception.InterfacesExceptionHandler
import com.noxcrew.interfaces.exception.StandardInterfacesExceptionHandler
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.transform.BlockingMode
import com.noxcrew.interfaces.transform.RefreshMode
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Stores all shared properties of an interface. */
public open class InterfaceProperties<P : Pane> {

    public companion object {
        /** All default reasons used for a new close handler. */
        public val DEFAULT_REASONS: List<InventoryCloseEvent.Reason> =
            InventoryCloseEvent.Reason.entries.minus(InventoryCloseEvent.Reason.PLUGIN)
    }

    private val _closeHandlers: MutableMap<InventoryCloseEvent.Reason, MutableList<CloseHandler>> = mutableMapOf()
    private val _clickPreprocessors: MutableCollection<ClickHandler> = mutableListOf()
    private val _preventedInteractions: MutableCollection<Action> = mutableListOf()

    // --- GENERAL ---
    /** The exception handler to use for this interface. */
    public var exceptionHandler: InterfacesExceptionHandler = StandardInterfacesExceptionHandler()

    /** The timeout to apply to all coroutines for this menu. */
    public var defaultTimeout: Duration = 2.5.seconds

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

    /** Whether clicking on empty slots should be cancelled. */
    public var preventClickingEmptySlots: Boolean = true

    /**
     * Whether the player's own inventory should be editable if [preventClickingEmptySlots] is `true`. Only allowed for
     * [ChestInterfaceBuilder].
     */
    public var allowClickingOwnInventoryIfClickingEmptySlotsIsPrevented: Boolean = false
        set(value) {
            field = if (this !is ChestInterfaceBuilder) {
                false
            } else {
                value
            }
        }

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
    /** Whether close handlers should be called when switching to a different view. */
    public var callCloseHandlerOnViewSwitch: Boolean = false

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

    /** Sets all values to their simple defaults. This is the default type! */
    public fun useSimpleDefaults() {
        redrawTitleOnReopen = true
        defaultRefreshMode = RefreshMode.ALWAYS
        defaultBlockMode = BlockingMode.ALWAYS
    }

    /** Sets all values to support efficient caching of components and minimize automatic refreshes. */
    public fun useCachingDefaults() {
        redrawTitleOnReopen = false
        defaultRefreshMode = RefreshMode.INITIAL
        defaultBlockMode = BlockingMode.INITIAL
    }

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
}
