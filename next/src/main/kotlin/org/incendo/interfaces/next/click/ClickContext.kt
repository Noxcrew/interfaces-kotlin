package org.incendo.interfaces.next.click

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.incendo.interfaces.next.view.InterfaceView

public data class ClickContext(
    public val player: Player,
    public val view: InterfaceView,
    public val type: ClickType,
    /** The hot bar slot pressed between 0-8 if [type] is [ClickType.NUMBER_KEY], or `-1` otherwise. */
    public val slot: Int
)
