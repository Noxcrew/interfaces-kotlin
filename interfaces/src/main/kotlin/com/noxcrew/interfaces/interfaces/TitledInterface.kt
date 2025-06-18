package com.noxcrew.interfaces.interfaces

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

/** The basis for an interface with a title. */
public interface TitledInterface {

    /** The title supplier of the interface. */
    public val titleSupplier: (suspend (Player) -> Component?)?
}
