package com.noxcrew.interfaces.view

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.interfaces.PlayerInterface
import com.noxcrew.interfaces.inventory.PlayerInterfacesInventory
import com.noxcrew.interfaces.pane.PlayerPane
import com.noxcrew.interfaces.utilities.runSync
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

    override fun title(value: Component) {
        error("PlayerInventoryView's cannot have a title")
    }

    override fun runChatQuery(timeout: Duration, onCancel: () -> Unit, onComplete: (Component) -> Unit) {
        error("PlayerInventoryView does not support chat queries")
    }

    override fun createInventory(): PlayerInterfacesInventory = PlayerInterfacesInventory(player)

    override fun openInventory() {
        // Close whatever inventory the player has open so they can look at their normal inventory!
        // This will only continue if the menu hasn't been closed yet.
        if (!isOpen()) {
            // First we close then we set the interface so we don't double open!
            InterfacesListeners.INSTANCE.setOpenInterface(player.uniqueId, null)
            player.closeInventory()
            InterfacesListeners.INSTANCE.setOpenInterface(player.uniqueId, this)
        }

        // Double-check that this inventory is open now!
        if (isOpen()) {
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
    }

    override suspend fun close(reason: InventoryCloseEvent.Reason, changingView: Boolean) {
        markClosed(reason, changingView)

        // Ensure we update the interface state in the main thread!
        // Even if the menu is not currently on the screen.
        runSync {
            InterfacesListeners.INSTANCE.setOpenInterface(player.uniqueId, null)
        }
    }

    override fun isOpen(): Boolean =
        (
            player.openInventory.type == InventoryType.CRAFTING ||
                player.openInventory.type == InventoryType.CREATIVE
            ) &&
            InterfacesListeners.INSTANCE.getOpenInterface(player.uniqueId) == this
}
