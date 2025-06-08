package com.noxcrew.interfaces.properties

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * The basis for an interfaces property that stores some larger state object.
 * State properties are refreshed whenever a view with one is rendered.
 *
 * The refresh method is intended to be overridden to load some larger state
 * object before triggering the trigger itself. When called on initialization
 * its trigger is ignored.
 */
public abstract class StateProperty(
    /** The minimum time to wait between automatic refreshes. */
    private val minimumRefreshTime: Duration = 30.seconds,
    private val delegate: DelegateTrigger = DelegateTrigger(),
) : Trigger by delegate {

    private var lastRefresh: Instant = Instant.MIN

    /** Performs a refresh of this property before its transform is rendered. Skips refresh if update was very recent.  */
    internal suspend fun initialize() {
        if (lastRefresh.plus(minimumRefreshTime.toJavaDuration()) > Instant.now()) return
        lastRefresh = Instant.now()
        refresh()
    }

    /**
     * Refreshes this property, updating before triggering the state.
     * Ignored if last refresh was within [debounce].
     */
    public suspend fun refresh(debounce: Duration = Duration.ZERO) {
        if (lastRefresh.plus(debounce.toJavaDuration()) > Instant.now()) return
        lastRefresh = Instant.now()
        update()
        trigger()
    }

    /** Updates the state. */
    protected abstract suspend fun update()
}
