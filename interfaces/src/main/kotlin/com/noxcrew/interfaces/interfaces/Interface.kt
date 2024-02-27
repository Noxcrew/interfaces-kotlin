package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

/** A created interface that can be opened for a player to create a unique view. */
public interface Interface<P : Pane> {

    /** The amount of rows this interface contains. */
    public val rows: Int

    /** All close handlers on this interface mapped by closing reason. */
    public val closeHandlers: Map<InventoryCloseEvent.Reason, CloseHandler>

    /** All transforms that make up this interface. */
    public val transforms: Collection<AppliedTransform<P>>

    /** A collection of click handlers that will be run before each click without blocking. */
    public val clickPreprocessors: Collection<ClickHandler>

    public val itemPostProcessor: ((ItemStack) -> Unit)?

    /** Returns the total amount of rows. */
    public fun totalRows(): Int = rows

    /** Creates a new pane based on this interface's type. */
    public fun createPane(): P

    /**
     * Opens an [InterfaceView] from this [Interface] for [player]. The [parent] defaults to whatever menu the player
     * is currently viewing. If [InterfaceView.back] is used [parent] will be the menu they return to.
     */
    public suspend fun open(
        player: Player,
        parent: InterfaceView? =
            InterfacesListeners.INSTANCE.convertHolderToInterfaceView(player.openInventory.topInventory.holder)
    ): InterfaceView
}
