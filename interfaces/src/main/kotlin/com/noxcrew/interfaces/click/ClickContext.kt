package com.noxcrew.interfaces.click

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
    /** The hot bar slot pressed between 0-8 if [type] is [ClickType.NUMBER_KEY], or `-1` otherwise. */
    public val slot: Int,
    /** Whether the click was fired from an interact event (not from an open inventory). */
    public val interact: Boolean
)
