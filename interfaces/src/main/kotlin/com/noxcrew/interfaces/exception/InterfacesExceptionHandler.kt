package com.noxcrew.interfaces.exception

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryCloseEvent

/** Controls how an exception in an interface is handled. */
public fun interface InterfacesExceptionHandler {

    /** Handles the given [exception]. */
    public suspend fun handleException(exception: Exception, context: InterfacesExceptionContext): InterfacesExceptionResolution

    /** Executes [function], reporting any errors to the [InterfacesExceptionHandler] being used. */
    public suspend fun <T> execute(
        context: InterfacesExceptionContext,
        onException: suspend (Exception, InterfacesExceptionResolution) -> Unit = { _, _ -> },
        function: suspend () -> T,
    ): T? {
        try {
            return function()
        } catch (x: TimeoutCancellationException) {
            // Timeout cancellation exceptions should trigger the handler!
            return handle(x, context, onException, function)
        } catch (x: CancellationException) {
            // Silently ignore job cancellation!
            return null
        } catch (x: Exception) {
            return handle(x, context, onException, function)
        }
    }

    /** Handles [exception] having occurred. */
    private suspend fun <T> handle(
        exception: Exception,
        context: InterfacesExceptionContext,
        onException: suspend (Exception, InterfacesExceptionResolution) -> Unit = { _, _ -> },
        function: suspend () -> T,
    ): T? {
        val resolution = handleException(exception, context)
        onException(exception, resolution)
        when (resolution) {
            InterfacesExceptionResolution.RETRY -> {
                // If the player has disconnected, always stop trying!
                // If an implementation returns retry for a disconnected player assume
                // they meant ignore.
                if (Bukkit.isStopping() || !context.player.isConnected) return null
                return execute(context.copy(retries = context.retries + 1), onException, function)
            }

            InterfacesExceptionResolution.CLOSE -> {
                context.view?.close(InventoryCloseEvent.Reason.PLUGIN)
                return null
            }

            InterfacesExceptionResolution.IGNORE -> return null
        }
    }
}
