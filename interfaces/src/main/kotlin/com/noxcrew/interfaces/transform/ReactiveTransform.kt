package com.noxcrew.interfaces.transform

import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.Trigger

/** A transform that updates whenever [triggers] are triggered. */
public interface ReactiveTransform<P : Pane> : Transform<P> {
    /** The triggers this transform listens to. */
    public val triggers: Array<Trigger>
}
