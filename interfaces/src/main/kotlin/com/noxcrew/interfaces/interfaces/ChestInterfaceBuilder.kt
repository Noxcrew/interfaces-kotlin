package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.Pane
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** Assists in building a [ChestInterface]. */
public class ChestInterfaceBuilder :
    InterfaceBuilder<Pane, ChestInterface>() {

    /** Sets the amount of rows for this interface to use. */
    public var rows: Int = 0

    /** Supplies the title of this interface. */
    public var titleSupplier: (suspend (Player) -> Component?)? = null

    override fun build(): ChestInterface = ChestInterface(
        rows,
        titleSupplier,
        this
    )
}
