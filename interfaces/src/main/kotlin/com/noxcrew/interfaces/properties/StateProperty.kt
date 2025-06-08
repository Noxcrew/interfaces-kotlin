package com.noxcrew.interfaces.properties

import com.noxcrew.interfaces.InterfacesConstants
import com.noxcrew.interfaces.exception.InterfacesExceptionContext
import com.noxcrew.interfaces.exception.InterfacesExceptionHandler
import com.noxcrew.interfaces.exception.InterfacesOperation
import com.noxcrew.interfaces.exception.StandardInterfacesExceptionHandler
import com.noxcrew.interfaces.utilities.InterfacesCoroutineDetails
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import org.bukkit.entity.Player
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
    /** The player this property is for. */
    public val player: Player,
    /** The minimum time to wait between automatic refreshes. */
    private val minimumRefreshTime: Duration = 30.seconds,
    /** The maximum time an update can take. */
    private val updateTimeout: Duration = 2.5.seconds,
    private val exceptionHandler: InterfacesExceptionHandler = StandardInterfacesExceptionHandler(),
    private val delegate: DelegateTrigger = DelegateTrigger(),
) : Trigger by delegate {

    private var updateJob: Deferred<Unit>? = null
    private var lastRefresh: Instant = Instant.MIN

    /** Performs a refresh of this property before its transform is rendered. Skips refresh if update was very recent.  */
    public suspend fun initialize() {
        if (lastRefresh.plus(minimumRefreshTime.toJavaDuration()) > Instant.now()) {
            if (updateJob != null) {
                updateJob?.await()
            }
            return
        }
        performUpdate()
    }

    /**
     * Refreshes this property, updating before triggering the state.
     * Ignored if last refresh was within [debounce].
     */
    public suspend fun refresh(debounce: Duration = Duration.ZERO) {
        if (lastRefresh.plus(debounce.toJavaDuration()) > Instant.now()) {
            if (updateJob != null) {
                updateJob?.await()
            }
            return
        }
        performUpdate {
            trigger()
        }
    }

    /** Performs the update, re-using the same job. */
    private suspend fun performUpdate(callback: () -> Unit = {}) {
        lastRefresh = Instant.now()

        if (updateJob != null) {
            updateJob?.await()
            callback()
            return
        }

        updateJob = InterfacesConstants.SCOPE.async(InterfacesCoroutineDetails(player.uniqueId, "running state property update")) {
            exceptionHandler.execute(
                InterfacesExceptionContext(
                    player,
                    InterfacesOperation.UPDATING_STATE,
                ),
            ) {
                withTimeout(updateTimeout) {
                    update()
                }
            }
            updateJob = null
        }
        updateJob?.await()
        callback()
    }

    /** Updates the state. */
    protected abstract suspend fun update()
}
