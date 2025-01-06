package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.CombinedPane
import net.kyori.adventure.text.Component

/** Assists in building a [CombinedInterface]. */
public class CombinedInterfaceBuilder :
    InterfaceBuilder<CombinedPane, CombinedInterface>() {

    /** Sets the amount of rows for this interface to use. */
    public var rows: Int = 0

    /** Sets the initial title of this interface. */
    public var initialTitle: Component? = null

    override fun build(): CombinedInterface = CombinedInterface(
        rows,
        initialTitle,
        this
    )
}
