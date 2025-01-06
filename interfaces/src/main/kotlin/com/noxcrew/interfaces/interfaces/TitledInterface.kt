package com.noxcrew.interfaces.interfaces

import net.kyori.adventure.text.Component

/** The basis for an interface with a title. */
public interface TitledInterface {
    /** The initial title of the interface. */
    public val initialTitle: Component?
}
