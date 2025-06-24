package com.noxcrew.interfaces.view

import com.noxcrew.interfaces.InterfacesListeners
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Represents a single view of an interface. The interface itself may be seen by multiple players,
 * but this view was created for a specific viewer.
 */
public interface InterfaceView {

    /** The player this view is for. */
    public val player: Player

    /**
     * Tracks whether this menu should be opened or not. This does not actually represent
     * whether the menu is open currently, it represents whether the code wants it to be
     * open. This gets set to `false` whenever the close handlers are called.
     *
     * This should be `false` whenever the menu is not on-screen.
     */
    public val shouldStillBeOpened: Boolean

    /**
     * Whether this view or any of its children are still opened. This can be used to verify whether
     * a parent view is still valid based on whether any of its children are open.
     */
    public val isTreeOpened: Boolean

    /** Opens up this view. */
    public suspend fun open(reload: Boolean = true)

    /** Re-opens only if this menu should still be. */
    public suspend fun reopenIfIntended(): Boolean {
        if (!shouldStillBeOpened) return false
        return reopen(null)
    }

    /**
     * Re-opens this view, if it hasn't yet been closed.
     *
     * If a new parent is given the menu is re-opened
     * only if the new parent is still open.
     */
    public suspend fun reopen(
        newParent: InterfaceView? =
            InterfacesListeners.INSTANCE.convertHolderToInterfaceView(player.openInventory.topInventory.getHolder(false)),
        reload: Boolean = true,
    ): Boolean

    /** Closes this view. */
    public suspend fun close(
        reason: InventoryCloseEvent.Reason = InventoryCloseEvent.Reason.UNKNOWN,
        changingView: Boolean = reason ==
            InventoryCloseEvent.Reason.OPEN_NEW,
    ): Unit = close(CoroutineScope(coroutineContext), reason, changingView)

    /** Closes this view immediately, running any closing handling on [coroutineScope]. */
    public fun close(
        coroutineScope: CoroutineScope,
        reason: InventoryCloseEvent.Reason = InventoryCloseEvent.Reason.UNKNOWN,
        changingView: Boolean = reason ==
            InventoryCloseEvent.Reason.OPEN_NEW,
    )

    /** Returns whether this view is opened based on the player's current shown inventory. */
    public fun isOpen(): Boolean

    /** Returns the parent view, or `null` if this view has no parent. */
    public fun parent(): InterfaceView?

    /** Re-opens the [parent] view, or closes this menu if it has no parent. */
    public suspend fun back()

    /** Sets the title fo this view to [value]. */
    public fun title(value: Component)

    /** Called whenever a view is opened. */
    public fun onOpen()

    /** Redraws all transforms in this view. */
    public fun redrawComplete()

    /** Ensures the decoration task is running. */
    public fun ensureDecorating()

    /**
     * Sends a chat query for this player. This view will temporarily close, allowing the player
     * to send a chat message. After a chat message is sent the result is passed to a predicate
     * and this view is re-opened. While this view is minimised it still respects all normal rules
     * and closing will still work properly. Opening any other inventory will end the query automatically.
     *
     * Will fail if this view is not open.
     */
    public fun runChatQuery(timeout: Duration = 30.seconds, onCancel: suspend () -> Unit = {}, onComplete: suspend (Component) -> Boolean)
}
