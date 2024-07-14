package com.noxcrew.interfaces.example

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.utilities.BoundInteger
import org.bukkit.Material

public class MovingExampleInterface : RegistrableInterface {
    override val subcommand: String = "moving"

    override fun create(): Interface<*, *> = buildChestInterface {
        val countProperty = BoundInteger(4, 1, 7)
        var count by countProperty

        // Allow clicking empty slots to allow testing various interactions with tiems and chest interfaces
        preventClickingEmptySlots = false

        rows = 1

        withTransform(countProperty) { pane, _ ->
            pane[0, 0] = StaticElement(drawable(Material.RED_CONCRETE)) { count-- }
            pane[0, 8] = StaticElement(drawable(Material.GREEN_CONCRETE)) { count++ }

            pane[0, count] = StaticElement(drawable(Material.STICK))
        }
    }
}
