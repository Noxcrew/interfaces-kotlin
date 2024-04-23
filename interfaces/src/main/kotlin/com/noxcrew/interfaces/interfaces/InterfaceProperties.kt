package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.transform.AppliedTransform
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

/** Stores all shared properties of an interface. */
public data class InterfaceProperties<P : Pane>(
    /** All close handlers on this interface mapped by closing reason. */
    public val closeHandlers: Map<InventoryCloseEvent.Reason, CloseHandler> = emptyMap(),
    /** All transforms that make up this interface. */
    public val transforms: Collection<AppliedTransform<P>> = emptySet(),
    /** A collection of click handlers that will be run before each click without blocking. */
    public val clickPreprocessors: Collection<ClickHandler> = emptySet(),
    /** A post-processor applied to all items placed in the inventory. */
    public val itemPostProcessor: ((ItemStack) -> Unit)? = {},
    /** Whether clicking on empty slots should be cancelled. */
    public val preventClickingEmptySlots: Boolean = false,
    /** All interactions that will be ignored on this view and cancelled on pane items without calling the handler. */
    public val preventedInteractions: Collection<Action> = emptySet(),
)
