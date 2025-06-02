package com.noxcrew.interfaces.exception

import kotlinx.coroutines.CancellationException
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryCloseEvent

/** Controls how an exception in an interface is handled. */
public fun interface InterfacesExceptionHandler {

    /** Handles the given [exception]. */
    public suspend fun handleException(exception: Exception, context: InterfacesExceptionContext): InterfacesExceptionResolution

    /** Executes [function], reporting any errors to the [InterfacesExceptionHandler] being used. */
    public suspend fun <T> execute(context: InterfacesExceptionContext, function: suspend () -> T,): T? {
        try {
            return function()
        } catch (x: CancellationException) {
            // Silently ignore job cancellation!
            return null
        } catch (x: Exception) {
            val resolution = handleException(x, context)
            when (resolution) {
                InterfacesExceptionResolution.RETRY -> {
                    // If the player has disconnected, always stop trying!
                    // If an implementation returns retry for a disconnected player assume
                    // they meant ignore.
                    if (Bukkit.isStopping() || !context.player.isConnected) return null
                    return execute(context.copy(retries = context.retries + 1), function)
                }

                InterfacesExceptionResolution.CLOSE -> {
                    context.view?.close(InventoryCloseEvent.Reason.PLUGIN)
                    return null
                }

                InterfacesExceptionResolution.IGNORE -> return null
            }
        }
    }
}
