package com.noxcrew.interfaces.click

import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

public data class ClickContext(
    public val player: Player,
    public val view: InterfaceView,
    public val type: ClickType,
    /** The hot bar slot pressed between 0-8 if [type] is [ClickType.NUMBER_KEY], or `-1` otherwise. */
    public val slot: Int
)
