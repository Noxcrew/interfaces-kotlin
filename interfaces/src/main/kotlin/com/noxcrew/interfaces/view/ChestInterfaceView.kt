package com.noxcrew.interfaces.view

import com.noxcrew.interfaces.interfaces.ChestInterface
import com.noxcrew.interfaces.inventory.ChestInterfacesInventory
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.utilities.TitleState
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/** Implements a chest view. */
public class ChestInterfaceView internal constructor(
    player: Player,
    backing: ChestInterface,
    parent: InterfaceView?
) : AbstractInterfaceView<ChestInterfacesInventory, ChestInterface, Pane>(
    player,
    backing,
    parent
),
    InventoryHolder {

    private val titleState = TitleState(backing.initialTitle)

    override fun title(value: Component) {
        titleState.current = value
    }

    override fun createInventory(): ChestInterfacesInventory = ChestInterfacesInventory(
        this,
        titleState.current,
        backing.rows
    )

    override fun openInventory() {
        player.openInventory(this.inventory)
    }

    override fun requiresPlayerUpdate(): Boolean = false

    override fun requiresNewInventory(): Boolean = super.requiresNewInventory() || titleState.hasChanged

    override fun getInventory(): Inventory = currentInventory.chestInventory

    override fun isOpen(): Boolean = player.openInventory.topInventory.holder == this
}
