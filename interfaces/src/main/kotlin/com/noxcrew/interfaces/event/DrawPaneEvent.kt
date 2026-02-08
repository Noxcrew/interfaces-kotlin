package com.noxcrew.interfaces.event

import com.noxcrew.interfaces.utilities.InventorySegment
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * An event emitted when the inventory of [player] is drawn.
 * If you want to listen to a specific view being drawn, add a panel post-processor
 * to the interface properties.
 */
public class DrawPaneEvent(
    player: Player,
    /** The view that was drawn. */
    public val view: InterfaceView,
    /** The segment being drawn to. */
    public val segment: InventorySegment,
) : PlayerEvent(player) {

    public companion object {
        @JvmStatic
        public val handlerList: HandlerList = HandlerList()
    }

    /** Whether any slots in the regular inventory were drawn. */
    public val isRegularInventory: Boolean
        get() = segment == InventorySegment.CONTAINER

    /** Whether any slots in the player inventory were drawn. */
    public val isPlayerInventory: Boolean
        get() = segment == InventorySegment.PLAYER

    override fun getHandlers(): HandlerList = handlerList
}
