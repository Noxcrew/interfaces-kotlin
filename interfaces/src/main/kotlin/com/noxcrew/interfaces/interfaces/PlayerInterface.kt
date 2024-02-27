package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.pane.PlayerPane
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.view.InterfaceView
import com.noxcrew.interfaces.view.PlayerInterfaceView
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

/** An interface that uses the entire player inventory. */
public class PlayerInterface internal constructor(
    override val closeHandlers: MutableMap<InventoryCloseEvent.Reason, CloseHandler>,
    override val transforms: Collection<AppliedTransform<PlayerPane>>,
    override val clickPreprocessors: Collection<ClickHandler>,
    override val itemPostProcessor: ((ItemStack) -> Unit)?
) : Interface<PlayerPane> {

    public companion object {
        /** The maximum number of rows for a player interface. */
        public const val MAX_NUMBER_OF_ROWS: Int = 9
    }

    override val rows: Int = 4

    override fun createPane(): PlayerPane = PlayerPane()

    override suspend fun open(player: Player, parent: InterfaceView?): PlayerInterfaceView {
        val view = PlayerInterfaceView(player, this)
        view.open()
        return view
    }
}
