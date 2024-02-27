package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.Pane

/** A generic builder for creating an [Interface]. */
public abstract class InterfaceBuilder<P : Pane, T : Interface<P>> {

    /** Creates the interface. */
    public abstract fun build(): T
}
