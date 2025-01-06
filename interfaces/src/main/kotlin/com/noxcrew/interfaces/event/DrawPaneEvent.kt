package com.noxcrew.interfaces.event

import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/** An event emitted when the inventory of [player] is drawn. */
public class DrawPaneEvent(
    player: Player,
    /** The view that was drawn. */
    public val view: InterfaceView,
    /** Whether any slots in the regular inventory were drawn. */
    public val isRegularInventory: Boolean,
    /** Whether any slots in the player inventory were drawn. */
    public val isPlayerInventory: Boolean,
) : PlayerEvent(player) {
    public companion object {
        @JvmStatic
        public val handlerList: HandlerList = HandlerList()
    }

    override fun getHandlers(): HandlerList = handlerList
}
