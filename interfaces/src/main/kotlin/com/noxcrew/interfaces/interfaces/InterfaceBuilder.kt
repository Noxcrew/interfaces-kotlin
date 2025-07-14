package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.Trigger
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.transform.BlockingMode
import com.noxcrew.interfaces.transform.ReactiveTransform
import com.noxcrew.interfaces.transform.RefreshMode
import com.noxcrew.interfaces.transform.StatefulTransform
import com.noxcrew.interfaces.transform.Transform
import com.noxcrew.interfaces.transform.builtin.PaginationTransformation
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
    public fun addTransform(
        /** The transform to add. */
        transform: Transform<P>,
        /** All additional triggers to refresh this transform. */
        vararg triggers: Trigger?,
        /** The blocking mode to use for this transform. */
        blocking: BlockingMode = defaultBlockMode,
        /** The refreshing mode to use for this transform. */
        refresh: RefreshMode = defaultRefreshMode,
    ): Unit = withTransform(triggers = triggers, blocking = blocking, refresh = refresh, transform = transform)

    /** Adds a new transform to the interface that updates whenever [triggers] change. */
    public fun withTransform(
        /** All additional triggers to refresh this transform. */
        vararg triggers: Trigger?,
        /** The blocking mode to use for this transform. */
        blocking: BlockingMode = defaultBlockMode,
        /** The refreshing mode to use for this transform. */
        refresh: RefreshMode = defaultRefreshMode,
        /** The transform to add. */
        transform: Transform<P>,
    ) {
        _transforms += AppliedTransform(
            blocking,
            refresh,
            transformCounter,
            buildSet {
                addAll(triggers.filterNotNull())
                if (transform is ReactiveTransform) {
                    addAll(transform.triggers.toList())
                }
                if (transform is StatefulTransform<*, *>) {
                    add(transform.property)
                }
            },
            transform,
        )
    }

    /** Helps with adding a paginated transform that should only re-render when its contents change. */
    public fun <E> addLazyPaginatedTransform(
        /** All additional triggers to refresh this transform. */
        vararg triggers: Trigger?,
        /** The blocking mode to use for this transform. */
        blocking: BlockingMode = defaultBlockMode,
        /** The transform to add. */
        transform: PaginationTransformation<P, E>,
        /** The reload method to update the values. */
        determineContents: suspend () -> List<E>,
    ) {
        addTransform(transform, triggers = triggers, blocking = blocking, refresh = RefreshMode.TRIGGER_ONLY)
        withTransform(blocking = blocking, refresh = RefreshMode.RELOAD) { _, _ ->
            transform.setContentsIfDifferent(determineContents())
        }
    }
}
