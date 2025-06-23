package com.noxcrew.interfaces.transform

import com.noxcrew.interfaces.interfaces.InterfaceBuilder
import com.noxcrew.interfaces.pane.CompletedPane
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.pane.complete
import com.noxcrew.interfaces.properties.Trigger
import com.noxcrew.interfaces.view.AbstractInterfaceView
import kotlinx.coroutines.withTimeout
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/** A transform that can been applied to a pane. */
public class AppliedTransform<P : Pane>(
    internal val blocking: BlockingMode,
    internal val stale: Boolean,
    internal val priority: Int,
    internal val triggers: Set<Trigger>,
    internal val backing: Transform<P>,
) : Transform<P> by backing {

    /** Stores different pre-completed states of this pane for different values of a stateful transform. */
    private val paneStates = ConcurrentHashMap<Any, CompletedPane>()

    /** Resets this transform. */
    public fun reset() {
        paneStates.clear()
    }

    /** Handles [trigger] being modified. */
    public fun handleChange(trigger: Trigger) {
        // Clear out the pane states whenever anything other than the main
        // property of a stateful transform has changed.
        if ((backing as? StatefulTransform<*, *>)?.property != trigger) {
            paneStates.clear()
        }
    }

    /** Returns the completed pane for this transform. */
    public suspend fun completePane(
        pane: P,
        player: Player,
        builder: InterfaceBuilder<P, *>,
        view: AbstractInterfaceView<*, *, P>,
    ): CompletedPane {
        // Determine the property of a possible stateful transform
        val property = (backing as? StatefulTransform<*, *>)?.property

        // Re-use a previously built pane for the main stateful property, or
        // create a new one within the timeout
        val completedPane = property?.let {
            val value by property
            paneStates[value ?: return@let null]
        } ?: withTimeout(builder.defaultTimeout) {
            invoke(pane, view)
            pane.complete(player)
        }

        // Store stateful panes
        property?.also {
            val value by property
            paneStates[value ?: return@also] = completedPane
        }

        return completedPane
    }
}
