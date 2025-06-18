package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.CombinedPane
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** Assists in building a [CombinedInterface]. */
public class CombinedInterfaceBuilder :
    InterfaceBuilder<CombinedPane, CombinedInterface>() {

    /** Sets the amount of rows for this interface to use. */
    public var rows: Int = 0

    /** Supplies the title of this interface. */
    public var titleSupplier: (suspend (Player) -> Component?)? = null

    override fun build(): CombinedInterface = CombinedInterface(
        rows,
        titleSupplier,
        this
    )
}
