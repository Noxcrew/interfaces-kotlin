package com.noxcrew.interfaces.event

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * An event emitted when the inventory of [player] is fully cleared.
 */
public class PlayerInventoryClearedEvent(player: Player) : PlayerEvent(player, !Bukkit.isPrimaryThread()) {

    public companion object {
        @JvmStatic
        public val handlerList: HandlerList = HandlerList()
    }

    override fun getHandlers(): HandlerList = handlerList
}
