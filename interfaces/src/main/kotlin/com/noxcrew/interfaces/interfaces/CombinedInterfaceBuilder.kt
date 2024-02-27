package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.CombinedPane
import net.kyori.adventure.text.Component

public class CombinedInterfaceBuilder :
    AbstractInterfaceBuilder<CombinedPane, CombinedInterface>() {

    public var rows: Int = 0
    public var initialTitle: Component? = null

    override fun build(): CombinedInterface = CombinedInterface(
        rows,
        initialTitle,
        closeHandlers,
        transforms,
        clickPreprocessors,
        itemPostProcessor
    )
}
