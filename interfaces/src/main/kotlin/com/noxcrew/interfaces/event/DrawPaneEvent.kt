package com.noxcrew.interfaces.event

import com.noxcrew.interfaces.interfaces.PlayerInventoryType
import com.noxcrew.interfaces.utilities.InventorySegment
import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.Bukkit
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
    /** The player inventory type used by this view. */
    public val playerInventoryType: PlayerInventoryType,
) : PlayerEvent(player, !Bukkit.isPrimaryThread()) {

    public companion object {
        @JvmStatic
        public val handlerList: HandlerList = HandlerList()
    }

    /** Whether any slots in the player inventory were drawn. If the menu is fake this is false, this method returns `true` if the player's real inventory is modified. */
    public val isPlayerInventory: Boolean
        get() = segment == InventorySegment.PLAYER && playerInventoryType != PlayerInventoryType.FAKE

    override fun getHandlers(): HandlerList = handlerList
}
