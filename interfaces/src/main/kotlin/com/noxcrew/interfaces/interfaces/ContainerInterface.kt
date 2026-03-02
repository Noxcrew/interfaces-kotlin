package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.grid.mapping.ContainerGridMapper
import com.noxcrew.interfaces.pane.ContainerPane
import com.noxcrew.interfaces.view.ContainerInterfaceView
import com.noxcrew.interfaces.view.InterfaceView
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** An interface that uses a container and possibly the player's inventory below. */
public abstract class ContainerInterface<I : ContainerInterface<I, P>, P : ContainerPane> internal constructor(
    override val builder: ContainerInterfaceBuilder<P, I>,
) : Interface<I, P>, TitledInterface {

    /** A simple instance of a container interface. */
    public class Simple(builder: ContainerInterfaceBuilder.Simple) :
        ContainerInterface<Simple, ContainerPane>(builder) {
        override fun createPane(): ContainerPane = ContainerPane(rows, includesPlayerInventory)
    }

    override val titleSupplier: (suspend (Player) -> Component?)? = builder.titleSupplier
    override val rows: Int = builder.rows
    override val includesPlayerInventory: Boolean = builder.playerInventoryType.includesPlayerInventory
    override val mapper: ContainerGridMapper = ContainerGridMapper(rows, includesPlayerInventory)

    override suspend fun open(player: Player, parent: InterfaceView?, reload: Boolean): ContainerInterfaceView<I, P> {
        val view = ContainerInterfaceView(player, this as I, parent)
        view.open(reload)
        return view
    }
}
