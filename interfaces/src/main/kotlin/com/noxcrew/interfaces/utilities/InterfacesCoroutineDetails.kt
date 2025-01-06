package com.noxcrew.interfaces.utilities

import java.util.UUID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** Context element that contains details used for error handling and debugging. */
internal data class InterfacesCoroutineDetails(
    internal val player: UUID?,
    internal val reason: String,
) : AbstractCoroutineContextElement(InterfacesCoroutineDetails) {
    /** Key this element. */
    internal companion object : CoroutineContext.Key<InterfacesCoroutineDetails>
}
