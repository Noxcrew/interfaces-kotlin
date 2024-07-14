package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.pane.CombinedPane
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player

/** A created interface that can be opened for a player to create a unique view. */
public interface Interface<I : Interface<I, P>, P : Pane> {

    /** The amount of rows this interface contains. */
    public val rows: Int

    /** The builder that creates this interface. */
    public val builder: InterfaceBuilder<P, I>

    /** Whether this view includes the player inventory. */
    public val includesPlayerInventory: Boolean

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

    /** Returns the [gridPoint] relative to the player's inventory within this inventory. */
    public fun relativizePlayerInventorySlot(gridPoint: GridPoint): GridPoint {
        // If it's a combined pane we offset by the chest size in rows to push the player
        // inventory slots to the bottom!
        val pane = createPane()
        if (pane is CombinedPane) {
            return GridPoint.at(pane.chestRows + gridPoint.x, gridPoint.y)
        }
        return gridPoint
    }
}
