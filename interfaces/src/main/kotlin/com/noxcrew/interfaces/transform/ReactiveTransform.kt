package com.noxcrew.interfaces.transform

import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.Trigger

public interface ReactiveTransform<P : Pane> : Transform<P> {

    public val triggers: Array<Trigger>
}
