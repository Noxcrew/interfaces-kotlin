package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player

/** A created interface that can be opened for a player to create a unique view. */
public interface Interface<P : Pane> {

    /** The amount of rows this interface contains. */
    public val rows: Int

    /** The properties of this interface. */
    public val properties: InterfaceProperties<P>

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
