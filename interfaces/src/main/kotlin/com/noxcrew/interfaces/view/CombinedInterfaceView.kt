package com.noxcrew.interfaces.view

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.interfaces.CombinedInterface
import com.noxcrew.interfaces.inventory.CombinedInterfacesInventory
import com.noxcrew.interfaces.pane.CombinedPane
import com.noxcrew.interfaces.utilities.TitleState
import net.kyori.adventure.text.Component
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

    private val titleState = TitleState()

    override val overlapsPlayerInventory: Boolean = true

    override fun title(value: Component) {
        titleState.current = value
    }

    override fun createInventory(): CombinedInterfacesInventory = CombinedInterfacesInventory(
        holder = this,
        player = player,
        title = titleState.current,
        rows = backing.rows,
        mapper = backing.mapper,
    )

    override fun openInventory() {
        player.openInventory(this.inventory)
        InterfacesListeners.INSTANCE.completeRendering(player.uniqueId, this)
    }

    override suspend fun updateTitle() {
        titleState.current = backing.titleSupplier?.invoke(player)
    }

    override fun requiresPlayerUpdate(): Boolean = false

    override fun requiresNewInventory(): Boolean = super.requiresNewInventory() || titleState.hasChanged

    override fun getInventory(): Inventory = currentInventory.chestInventory

    override fun isOpen(): Boolean = player.openInventory.topInventory.getHolder(false) == this
}
