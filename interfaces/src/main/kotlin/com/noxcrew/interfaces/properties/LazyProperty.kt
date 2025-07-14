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

/** A property that is lazily evaluated. */
public abstract class LazyProperty<T : Any>(
    /** The player this property is for. */
    public val player: Player,
    /** The maximum time an update can take. */
    private val updateTimeout: Duration = 5.seconds,
    private val exceptionHandler: InterfacesExceptionHandler = StandardInterfacesExceptionHandler(),
) : DelegateTrigger() {
    private var updateJob: Deferred<Unit>? = null
    private var triggeringUpdate = false
    private var lastRefresh: Instant = Instant.MIN
    private var initialized: Boolean = false

    /** A simple lazy property backed by [block]. */
    public class Simple<T : Any>(
        player: Player,
        updateTimeout: Duration = 5.seconds,
        exceptionHandler: StandardInterfacesExceptionHandler,
        private val block: suspend (Boolean) -> T,
    ) : LazyProperty<T>(player, updateTimeout, exceptionHandler) {
        override suspend fun load(reload: Boolean): T = block(reload)
    }

    /** The current value of this property. */
    protected var value: T? = null

    /** Resets this value, requiring it to be reloaded. */
    public fun reset() {
        value = null
    }

    /** Loads the value of this property. */
    public abstract suspend fun load(reload: Boolean = true): T

    /** Initializes this property if it hasn't already. */
    public suspend fun initialize(view: InterfaceView? = null) {
        if (!initialized || value == null) {
            reevaluate(reload = !initialized, view = view)
        }
    }

    /**
     * Triggers a re-evaluation of the property.
     * If [reload] is given, all data should be fully reloaded.
     */
    public suspend fun reevaluate(
        reload: Boolean = true,
        trigger: Boolean = true,
        debounce: Duration = 200.milliseconds,
        view: InterfaceView? = null
    ): T {
        // Mark down if we want an update to happen
        if (trigger) {
            triggeringUpdate = true
        }

        // Await any job if we are already updating
        if (updateJob != null) {
            updateJob?.await()
            return value ?: load(reload)
        }

        // If we have recently refreshed, re-use the value!
        if (lastRefresh.plus(debounce.toJavaDuration()) > Instant.now()) {
            return value ?: load(reload)
        }

        updateJob = InterfacesConstants.SCOPE.async(InterfacesCoroutineDetails(player.uniqueId, "running state property update")) {
            exceptionHandler.execute(
                InterfacesExceptionContext(
                    player,
                    InterfacesOperation.UPDATING_LAZY,
                    view,
                ),
            ) {
                // Make any failed reload of the value cause it to be refreshed again later!
                initialized = false

                withTimeout(updateTimeout) {
                    value = load(reload)
                    if (triggeringUpdate) {
                        trigger()
                        triggeringUpdate = false
                    }

                    // Only mark initialization done after we finish an update!
                    initialized = true
                }
            }
            lastRefresh = Instant.now()
            updateJob = null
        }
        updateJob?.await()
        return value ?: load(reload)
    }

    /** Returns the value of this property. */
    public suspend fun getValue(): T = value ?: reevaluate(false)

    /** Returns the cached value of this property. */
    public fun getCachedValue(): T? = value
}
