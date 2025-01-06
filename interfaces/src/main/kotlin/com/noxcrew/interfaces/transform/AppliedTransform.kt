package com.noxcrew.interfaces.transform

import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.Trigger

/** A transform that can been applied to a pane. */
public class AppliedTransform<P : Pane>(
    internal val priority: Int,
    internal val triggers: Set<Trigger>,
    transform: Transform<P>,
) : Transform<P> by transform
