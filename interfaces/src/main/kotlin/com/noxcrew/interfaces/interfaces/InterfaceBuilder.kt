package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.Trigger
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.transform.BlockingMode
import com.noxcrew.interfaces.transform.ReactiveTransform
import com.noxcrew.interfaces.transform.StatefulTransform
import com.noxcrew.interfaces.transform.Transform
import com.noxcrew.interfaces.utilities.IncrementingInteger

/** Assists in creating a new interface. */
public abstract class InterfaceBuilder<P : Pane, I : Interface<I, P>> : InterfaceProperties<P>() {

    private val transformCounter by IncrementingInteger()
    private val _transforms: MutableCollection<AppliedTransform<P>> = mutableListOf()

    /** All transforms in this builder. */
    public val transforms: Collection<AppliedTransform<P>>
        get() = _transforms

    /** Creates the interface. */
    public abstract fun build(): I

    /** Adds a new transform to the interface that updates whenever [triggers] change. */
    public fun withTransform(
        vararg triggers: Trigger?,
        /** Whether the contents of this transform should prevent the initial render from completing. */
        blocking: BlockingMode = BlockingMode.INITIAL,
        /** Whether this transform can be left the same when re-opening a menu. */
        stale: Boolean = false,
        transform: Transform<P>,
    ) {
        _transforms += AppliedTransform(blocking, stale, transformCounter, triggers.filterNotNull().toSet(), transform)
    }

    /** Adds a new reactive transform to the interface. */
    public fun addTransform(
        reactiveTransform: ReactiveTransform<P>,
        /** Whether the contents of this transform should prevent the initial render from completing. */
        blocking: BlockingMode = BlockingMode.INITIAL,
        /** Whether this transform can be left the same when re-opening a menu. */
        stale: Boolean = false,
    ) {
        _transforms += AppliedTransform(blocking, stale, transformCounter, reactiveTransform.triggers.toSet(), reactiveTransform)
    }

    /** Adds a new stateful transform to the interface. */
    public fun <T> addTransform(
        statefulTransform: StatefulTransform<P, T>,
        vararg triggers: Trigger?,
        /** Whether the contents of this transform should prevent the initial render from completing. */
        blocking: BlockingMode = BlockingMode.INITIAL,
        /** Whether this transform can be left the same when re-opening a menu. */
        stale: Boolean = false,
    ) {
        _transforms += AppliedTransform(
            blocking,
            stale,
            transformCounter,
            setOf(statefulTransform.property).plus(triggers.filterNotNull().toSet()),
            statefulTransform,
        )
    }
}
