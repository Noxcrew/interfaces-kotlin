package com.noxcrew.interfaces.view

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.interfaces.CombinedInterface
import com.noxcrew.interfaces.inventory.CombinedInterfacesInventory
import com.noxcrew.interfaces.pane.CombinedPane
import com.noxcrew.interfaces.utilities.TitleState
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.event.CraftEventFactory
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/** Implements a combined view. */
public class CombinedInterfaceView internal constructor(
    player: Player,
    backing: CombinedInterface,
    parent: InterfaceView?,
) : AbstractInterfaceView<CombinedInterfacesInventory, CombinedInterface, CombinedPane>(
    player,
    backing,
    parent,
),
    InventoryHolder {
    private companion object {
        /** All possible menu types. */
        private val TYPES: List<MenuType<*>> = listOf(
            MenuType.GENERIC_9x1,
            MenuType.GENERIC_9x2,
            MenuType.GENERIC_9x3,
            MenuType.GENERIC_9x4,
            MenuType.GENERIC_9x5,
            MenuType.GENERIC_9x6,
        )
    }

    private val titleState = TitleState()

    override fun title(): Component? = titleState.current

    override fun title(value: Component) {
        titleState.current = value
    }

    override fun createInventory(): CombinedInterfacesInventory = CombinedInterfacesInventory(
        holder = this,
        player = player,
        rows = backing.rows,
        mapper = backing.mapper,
    )

    override fun openInventory() {
        val nmsPlayer = (player as CraftPlayer).handle
        if (nmsPlayer.containerMenu !== nmsPlayer.inventoryMenu) {
            nmsPlayer.connection.handleContainerClose(
                ServerboundContainerClosePacket(nmsPlayer.containerMenu.containerId),
                org.bukkit.event.inventory.InventoryCloseEvent.Reason.OPEN_NEW,
            )
        }
        var container: AbstractContainerMenu = ChestMenu(
            TYPES[backing.rows - 1],
            nmsPlayer.nextContainerCounter(),
            currentInventory.playerInventory,
            currentInventory.chestInventory,
            backing.rows,
        )
        val result = CraftEventFactory.callInventoryOpenEventWithTitle(nmsPlayer, container)
        container = result.second ?: return

        // Inform the client to open up the screen, then initialize the container!
        if (!nmsPlayer.isImmobile) {
            nmsPlayer.connection.send(
                ClientboundOpenScreenPacket(
                    container.containerId,
                    container.type,
                    PaperAdventure.asVanilla(result.first ?: title() ?: Component.empty()),
                ),
            )
        }
        nmsPlayer.containerMenu = container
        nmsPlayer.initMenu(container)

        // Send the off-hand item in the custom inventory
        nmsPlayer.connection.send(
            ClientboundContainerSetSlotPacket(
                nmsPlayer.inventoryMenu.containerId,
                nmsPlayer.inventoryMenu.incrementStateId(),
                net.minecraft.world.inventory.InventoryMenu.SHIELD_SLOT,
                currentInventory.playerInventory.equipment.get(EquipmentSlot.OFFHAND),
            ),
        )

        // Mark down that we've finished rendering this menu
        InterfacesListeners.INSTANCE.completeRendering(player.uniqueId, this)
    }

    override suspend fun updateTitle() {
        titleState.current = backing.titleSupplier?.invoke(player)
    }

    override fun requiresNewInventory(): Boolean = super.requiresNewInventory() || titleState.hasChanged

    override fun getInventory(): Inventory = currentInventory.bukkitInventory

    override fun isOpen(): Boolean = player.openInventory.topInventory.getHolder(false) == this
}
