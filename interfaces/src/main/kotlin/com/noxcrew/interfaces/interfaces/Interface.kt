package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

public interface Interface<P : Pane> {

    public val rows: Int

    public val closeHandlers: MutableMap<InventoryCloseEvent.Reason, CloseHandler>

    public val transforms: Collection<AppliedTransform<P>>

    public val clickPreprocessors: Collection<ClickHandler>

    public val itemPostProcessor: ((ItemStack) -> Unit)?

    public fun totalRows(): Int = rows

    public fun createPane(): P

    /**
     * Opens an [InterfaceView] from this [Interface]. The parent defaults to whatever menu the player
     * is currently viewing.
     *
     * @param player the player to show the view
     * @param parent the parent view that is opening the interface
     * @return the view
     */
    public suspend fun open(
        player: Player,
        parent: InterfaceView? =
            InterfacesListeners.INSTANCE.convertHolderToInterfaceView(player.openInventory.topInventory.holder)
    ): InterfaceView
}
