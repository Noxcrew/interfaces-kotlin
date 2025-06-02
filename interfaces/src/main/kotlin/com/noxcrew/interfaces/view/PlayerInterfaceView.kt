package com.noxcrew.interfaces.view

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.interfaces.PlayerInterface
import com.noxcrew.interfaces.inventory.PlayerInterfacesInventory
import com.noxcrew.interfaces.pane.PlayerPane
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import kotlin.time.Duration

/** Implements a player interface view. */
public class PlayerInterfaceView internal constructor(
    player: Player,
    backing: PlayerInterface
) : AbstractInterfaceView<PlayerInterfacesInventory, PlayerInterface, PlayerPane>(
    player,
    backing,
    null
) {

    override val overlapsPlayerInventory: Boolean = true

    override fun title(value: Component) {
        error("PlayerInventoryView's cannot have a title")
    }

    override fun runChatQuery(timeout: Duration, onCancel: suspend () -> Unit, onComplete: suspend (Component) -> Boolean) {
        error("PlayerInventoryView does not support chat queries")
    }

    override fun createInventory(): PlayerInterfacesInventory = PlayerInterfacesInventory(player, backing.mapper)

    override fun openInventory() {
        // Close whatever inventory the player has open so they can look at their normal inventory!
        // This will only continue if the menu hasn't been closed yet.
        if (!isOpen()) {
            // Remove this inventory from the background interface before closing so it
            // doesn't automatically re-open!
            InterfacesListeners.INSTANCE.markViewClosed(player.uniqueId, this)
            player.closeInventory()
        }

        // Open this player interface for the player
        InterfacesListeners.INSTANCE.setOpenView(player.uniqueId, this)

        if (!builder.inheritExistingItems) {
            // Clear the player's inventory!
            player.inventory.clear()
            if (player.openInventory.topInventory.type == InventoryType.CRAFTING ||
                player.openInventory.topInventory.type == InventoryType.CREATIVE
            ) {
                player.openInventory.topInventory.clear()
            }
            player.openInventory.setCursor(null)
        }

        // Trigger onOpen manually because there is no real inventory being opened,
        // this will also re-draw the player inventory parts!
        onOpen()
    }

    override fun close(coroutineScope: CoroutineScope, reason: InventoryCloseEvent.Reason, changingView: Boolean) {
        markClosed(coroutineScope, reason, changingView)
    }

    override fun isOpen(): Boolean = InterfacesListeners.INSTANCE.getOpenPlayerInterface(player.uniqueId) == this
}
