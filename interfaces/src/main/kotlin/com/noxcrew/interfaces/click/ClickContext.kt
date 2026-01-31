package com.noxcrew.interfaces.click

import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/** The context provided to a click on an interface. */
public data class ClickContext(
    /** The player that caused the click. */
    public val player: Player,
    /** The view that was clicked on. */
    public val view: InterfaceView,
    /** The type of click that was performed. */
    public val type: ClickType,
    /** The slot being clicked on. */
    public val slot: GridPoint,
    /** Whether the click was fired from an interact event (not from an open inventory). */
    public val interact: Boolean,
)
