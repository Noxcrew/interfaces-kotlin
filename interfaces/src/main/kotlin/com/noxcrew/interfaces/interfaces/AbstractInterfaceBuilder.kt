package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.Trigger
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.transform.ReactiveTransform
import com.noxcrew.interfaces.transform.Transform
import com.noxcrew.interfaces.utilities.IncrementingInteger
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

/** Assists in creating a new interface. */
public abstract class AbstractInterfaceBuilder<P : Pane, I : Interface<P>> internal constructor() : InterfaceBuilder<P, I>() {

    private companion object {
        /** All default reasons used for a new close handler. */
        private val DEFAULT_REASONS = InventoryCloseEvent.Reason.values().toList().minus(InventoryCloseEvent.Reason.PLUGIN)
    }

    private val transformCounter by IncrementingInteger()

    protected val closeHandlers: MutableMap<InventoryCloseEvent.Reason, CloseHandler> = mutableMapOf()
    protected val transforms: MutableCollection<AppliedTransform<P>> = mutableListOf()
    protected val clickPreprocessors: MutableCollection<ClickHandler> = mutableListOf()
    protected val preventedInteractions: MutableCollection<Action> = mutableListOf()

    /** Sets an item post processor to apply to every item in the interface. */
    public var itemPostProcessor: ((ItemStack) -> Unit)? = null

    /** Whether clicking on empty slots should be cancelled. */
    public var preventClickingEmptySlots: Boolean = false

    /** The properties object to use for the created interface. */
    public val properties: InterfaceProperties<P>
        get() = InterfaceProperties(
            closeHandlers,
            transforms,
            clickPreprocessors,
            itemPostProcessor,
            preventClickingEmptySlots,
            preventedInteractions,
        )

    /** Adds a new transform to the interface that updates whenever [triggers] change. */
    public fun withTransform(vararg triggers: Trigger, transform: Transform<P>) {
        transforms += AppliedTransform(transformCounter, triggers.toSet(), transform)
    }

    /** Adds a new reactive transform to the interface. */
    public fun addTransform(reactiveTransform: ReactiveTransform<P>) {
        transforms += AppliedTransform(transformCounter, reactiveTransform.triggers.toSet(), reactiveTransform)
    }

    /** Adds a new close handler [closeHandler] that triggers whenever the inventory is closed for any of the given [reasons]. */
    public fun withCloseHandler(
        reasons: Collection<InventoryCloseEvent.Reason> = DEFAULT_REASONS,
        closeHandler: CloseHandler
    ) {
        reasons.forEach {
            closeHandlers[it] = closeHandler
        }
    }

    /** Adds a new pre-processor to this menu which will run [handler] before every click without blocking. */
    public fun withPreprocessor(handler: ClickHandler) {
        clickPreprocessors += handler
    }

    /** Adds [action] to be cancelled without triggering any click handlers on valid items in this pane. */
    public fun withPreventedAction(action: Action) {
        preventedInteractions += action
    }
}
