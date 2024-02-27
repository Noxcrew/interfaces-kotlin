package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.Pane

public abstract class InterfaceBuilder<P : Pane, T : Interface<P>> {

    public abstract fun build(): T
}
