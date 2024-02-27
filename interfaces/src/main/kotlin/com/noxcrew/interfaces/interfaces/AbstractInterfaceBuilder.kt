package com.noxcrew.interfaces.interfaces

import com.noxcrew.interfaces.click.ClickHandler
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.Trigger
import com.noxcrew.interfaces.transform.AppliedTransform
import com.noxcrew.interfaces.transform.ReactiveTransform
import com.noxcrew.interfaces.transform.Transform
import com.noxcrew.interfaces.utilities.IncrementingInteger
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

public abstract class AbstractInterfaceBuilder<P : Pane, I : Interface<P>> internal constructor() :
    InterfaceBuilder<P, I>() {

    private companion object {
        private val DEFAULT_REASONS = InventoryCloseEvent.Reason.values().toList().minus(InventoryCloseEvent.Reason.PLUGIN)
    }

    private val transformCounter by IncrementingInteger()

    protected val closeHandlers: MutableMap<InventoryCloseEvent.Reason, CloseHandler> = mutableMapOf()
    protected val transforms: MutableCollection<AppliedTransform<P>> = mutableListOf()
    protected val clickPreprocessors: MutableCollection<ClickHandler> = mutableListOf()

    public var itemPostProcessor: ((ItemStack) -> Unit)? = null

    public fun withTransform(vararg triggers: Trigger, transform: Transform<P>) {
        transforms.add(AppliedTransform(transformCounter, triggers.toSet(), transform))
    }

    public fun addTransform(reactiveTransform: ReactiveTransform<P>) {
        transforms.add(AppliedTransform(transformCounter, reactiveTransform.triggers.toSet(), reactiveTransform))
    }

    public fun withCloseHandler(
        reasons: Collection<InventoryCloseEvent.Reason> = DEFAULT_REASONS,
        closeHandler: CloseHandler
    ) {
        reasons.forEach {
            closeHandlers[it] = closeHandler
        }
    }

    public fun withPreprocessor(handler: ClickHandler) {
        clickPreprocessors += handler
    }
}
