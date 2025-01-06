package com.noxcrew.interfaces.click

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.cancel

/** Handles a click action. Receives a [ClickContext] object filled with information about the click. */
public fun interface ClickHandler {
    public companion object {
        /**
         * An empty click handler that performs no logic. The click event will be cancelled
         * which prevents the item from being moved.
         */
        public val EMPTY: ClickHandler = ClickHandler { }

        /**
         * An empty click handler that does not cancel the click event. This allows the item
         * to be removed from the inventory.
         */
        @Deprecated("Will be replaced with a property of the item in a future version")
        public val ALLOW: ClickHandler = ClickHandler { cancelled = false }

        /** Runs a [CompletableClickHandler] with [clickHandler] and [context]. */
        public fun process(
            clickHandler: ClickHandler,
            context: ClickContext,
        ): Unit =
            with(clickHandler) {
                CompletableClickHandler().handle(context)
            }
    }

    /** Handles a click with the given [context]. */
    public fun CompletableClickHandler.handle(context: ClickContext)
}

/** A click handler that be assigned tasks to run on completion. */
public class CompletableClickHandler {
    private val deferred = CompletableDeferred<Unit>(null)

    /** Whether this handler has been completed (or cancelled). */
    public val completed: Boolean
        get() = deferred.isCancelled || deferred.isCompleted

    /** Whether the base click event should be cancelled. */
    @Deprecated("Will be replaced with a property of the item in a future version")
    public var cancelled: Boolean = true

    /**
     * Whether this handler is completing later. If `true`, any further click interactions on
     * the inventory are blocked until this handler completes. It is advised to only use this
     * if you run further logic such that [complete] is always called, even if an exception
     * occurs.
     */
    public var completingLater: Boolean = false

    /** Completes this click handler. Ignored if the handler is already completed. */
    public fun complete(): Boolean {
        if (completed) return false
        return deferred.complete(Unit)
    }

    /** Cancels this click handler with an exception. Ignored if the handler is already completed. */
    public fun cancel() {
        if (completed) return
        deferred.cancel("Cancelled click handler after 6s timeout")
    }

    /** Adds [handler] to be invoked when this handler completes. */
    public fun onComplete(handler: CompletionHandler): CompletableClickHandler {
        deferred.invokeOnCompletion(handler)
        return this
    }
}
