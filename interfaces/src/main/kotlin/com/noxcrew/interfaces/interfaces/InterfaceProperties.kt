package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.pane.Pane
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

/** Stores all shared properties of an interface. */
public open class InterfaceProperties<P : Pane> {

    private companion object {
        /** All default reasons used for a new close handler. */
        private val DEFAULT_REASONS = InventoryCloseEvent.Reason.entries.minus(InventoryCloseEvent.Reason.PLUGIN)
    }

    private val _closeHandlers: MutableMap<InventoryCloseEvent.Reason, CloseHandler> = mutableMapOf()
    private val _clickPreprocessors: MutableCollection<ClickHandler> = mutableListOf()
    private val _preventedInteractions: MutableCollection<Action> = mutableListOf()

    /** All close handlers on this interface mapped by closing reason. */
    public val closeHandlers: Map<InventoryCloseEvent.Reason, CloseHandler>
        get() = _closeHandlers

    /** A collection of click handlers that will be run before each click without blocking. */
    public val clickPreprocessors: Collection<ClickHandler>
        get() = _clickPreprocessors

    /** All interactions that will be ignored on this view and cancelled on pane items without calling the handler. */
    public val preventedInteractions: Collection<Action>
        get() = _preventedInteractions

    /** A post-processor applied to all items placed in the inventory. */
    public var itemPostProcessor: ((ItemStack) -> Unit)? = {}

    /** Whether clicking on empty slots should be cancelled. */
    public var preventClickingEmptySlots: Boolean = true

    /**
     * Persists items added to this pane in a previous instance.
     * Particularly useful for player inventories, this allows the non-interface items
     * to function as normal inventory items and be normally added/removed.
     */
    public var persistAddedItems: Boolean = false

    /** Keeps items that were previously in the inventory before opening this. */
    public var inheritExistingItems: Boolean = false

    /** Whether close handlers should be called when switching to a different view. */
    public var callCloseHandlerOnViewSwitch: Boolean = true

    /** Whether to place empty air elements in all background slots. */
    public var fillMenuWithAir: Boolean = false

    /** Adds a new close handler [closeHandler] that triggers whenever the inventory is closed for any of the given [reasons]. */
    public fun addCloseHandler(
        reasons: Collection<InventoryCloseEvent.Reason> = DEFAULT_REASONS,
        closeHandler: CloseHandler
    ) {
        reasons.forEach {
            _closeHandlers[it] = closeHandler
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
