package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.PlayerPane

/** Assists in building a [PlayerInterface]. */
public class PlayerInterfaceBuilder : AbstractInterfaceBuilder<PlayerPane, PlayerInterface>() {

    override fun build(): PlayerInterface = PlayerInterface(properties)
}
