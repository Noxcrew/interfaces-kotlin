package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.PlayerPane

public class PlayerInterfaceBuilder :
    AbstractInterfaceBuilder<PlayerPane, PlayerInterface>() {

    override fun build(): PlayerInterface = PlayerInterface(
        closeHandlers,
        transforms,
        clickPreprocessors,
        itemPostProcessor
    )
}
