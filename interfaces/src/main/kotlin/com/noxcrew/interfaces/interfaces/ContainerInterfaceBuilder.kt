package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.ContainerPane
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** Assists in building a container interface. */
public abstract class ContainerInterfaceBuilder<P : ContainerPane, I : ContainerInterface<I, P>> : InterfaceBuilder<P, I>() {

    public companion object {
        /** The maximum number of rows for a default container GUI. */
        public const val MAX_NUMBER_OF_ROWS: Int = 6
    }

    /** A simple instance of a chest-based container. */
    public class Simple : ContainerInterfaceBuilder<ContainerPane, ContainerInterface.Simple>() {
        override fun build(): ContainerInterface.Simple = ContainerInterface.Simple(this)
    }

    /** Sets the amount of rows for this interface to use. */
    public var rows: Int = 0

    /** The type of player inventory being included. */
    public var playerInventoryType: PlayerInventoryType = PlayerInventoryType.DEFAULT

    /** Supplies the title of this interface. */
    public var titleSupplier: (suspend (Player) -> Component?)? = null
}
