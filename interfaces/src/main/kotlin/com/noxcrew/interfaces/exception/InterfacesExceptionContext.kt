package com.noxcrew.interfaces.exception

import com.noxcrew.interfaces.view.InterfaceView
import org.bukkit.entity.Player

/** Stores context on an exception in interfaces. */
public data class InterfacesExceptionContext(
    /** The player owning the interface. */
    public val player: Player,
    /** The operation being attempted. */
    public val operation: InterfacesOperation,
    /** How often this particular action has been retried. */
    public val retries: Int = 0,
    /** The view involved in the exception. */
    public val view: InterfaceView? = null,
)
