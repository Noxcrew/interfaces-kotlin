package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.Trigger
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.transform.ReactiveTransform
import com.noxcrew.interfaces.transform.Transform
import com.noxcrew.interfaces.transform.TransformType
import com.noxcrew.interfaces.utilities.IncrementingInteger

/** Assists in creating a new interface. */
public abstract class InterfaceBuilder<P : Pane, I : Interface<I, P>> : InterfaceProperties<P>() {

    private val transformCounter by IncrementingInteger()
    private val _transforms: MutableCollection<AppliedTransform<P>> = mutableListOf()
    private var idCounter = 0

    /** All transforms in this builder. */
    public val transforms: Collection<AppliedTransform<P>>
        get() = _transforms

    /** Creates the interface. */
    public abstract fun build(): I

    /** Adds a new transform to the interface that updates whenever [triggers] change. */
    public fun withTransform(
        vararg triggers: Trigger,
        type: TransformType = TransformType.REGULAR,
        debugId: String = (idCounter++).toString(),
        transform: Transform<P>
    ) {
        _transforms += AppliedTransform(debugId, type, transformCounter, triggers.toSet(), transform)
    }

    /** Adds a new reactive transform to the interface. */
    public fun addTransform(
        reactiveTransform: ReactiveTransform<P>,
        type: TransformType = TransformType.REGULAR,
        debugId: String = (idCounter++).toString()
    ) {
        _transforms += AppliedTransform(debugId, type, transformCounter, reactiveTransform.triggers.toSet(), reactiveTransform)
    }
}
