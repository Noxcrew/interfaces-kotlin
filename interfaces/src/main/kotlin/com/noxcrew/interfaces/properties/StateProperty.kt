package com.noxcrew.interfaces.properties

import com.noxcrew.interfaces.InterfacesConstants
import com.noxcrew.interfaces.exception.InterfacesExceptionContext
import com.noxcrew.interfaces.exception.InterfacesExceptionHandler
import com.noxcrew.interfaces.exception.InterfacesOperation
import com.noxcrew.interfaces.exception.StandardInterfacesExceptionHandler
import com.noxcrew.interfaces.utilities.InterfacesCoroutineDetails
import com.noxcrew.interfaces.view.InterfaceView
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import org.bukkit.entity.Player
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
    /** The maximum time an update can take. */
    private val updateTimeout: Duration = 2.5.seconds,
    private val exceptionHandler: InterfacesExceptionHandler = StandardInterfacesExceptionHandler(),
    private val delegate: DelegateTrigger = DelegateTrigger(),
) : Trigger by delegate {

    private var updateJob: Deferred<Unit>? = null
    private var lastRefresh: Instant = Instant.MIN
    private var initialized: Boolean = false

    /** Initializes this property if it hasn't already. */
    public suspend fun initialize(view: InterfaceView? = null) {
        if (!initialized) {
            refresh(view = view)
        }
    }

    /**
     * Refreshes this property, updating before triggering the state.
     * Ignored if last refresh was within [debounce].
     */
    public suspend fun refresh(debounce: Duration = 200.milliseconds, view: InterfaceView? = null) {
        // Await any existing job if one is running
        if (updateJob != null) {
            updateJob?.await()
            return
        }

        // Avoid refreshing too often
        if (lastRefresh.plus(debounce.toJavaDuration()) > Instant.now()) return

        updateJob = InterfacesConstants.SCOPE.async(InterfacesCoroutineDetails(player.uniqueId, "running state property update")) {
            exceptionHandler.execute(
                InterfacesExceptionContext(
                    player,
                    InterfacesOperation.UPDATING_STATE,
                    view,
                ),
            ) {
                withTimeout(updateTimeout) {
                    update()
                }
            }

            // Start the timeout after we finish!
            initialized = true
            lastRefresh = Instant.now()
            updateJob = null
        }
        updateJob?.await()
        trigger()
    }

    /** Updates the state. */
    protected abstract suspend fun update()
}
