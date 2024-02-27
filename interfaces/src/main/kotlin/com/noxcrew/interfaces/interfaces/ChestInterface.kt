package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.view.ChestInterfaceView
import com.noxcrew.interfaces.view.InterfaceView
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

/** An interface that uses a chest GUI. */
public class ChestInterface internal constructor(
    override val rows: Int,
    override val initialTitle: Component?,
    override val closeHandlers: MutableMap<InventoryCloseEvent.Reason, CloseHandler>,
    override val transforms: Collection<AppliedTransform<Pane>>,
    override val clickPreprocessors: Collection<ClickHandler>,
    override val itemPostProcessor: ((ItemStack) -> Unit)?
) : Interface<Pane>, TitledInterface {

    public companion object {
        /** The maximum number of rows for a chest GUI. */
        public const val MAX_NUMBER_OF_ROWS: Int = 6
    }

    override fun createPane(): Pane = Pane()

    override suspend fun open(player: Player, parent: InterfaceView?): ChestInterfaceView {
        val view = ChestInterfaceView(player, this, parent)
        view.open()
        return view
    }
}
