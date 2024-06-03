package com.noxcrew.interfaces.example

import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.buildCombinedInterface
import com.noxcrew.interfaces.properties.interfaceProperty
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

public class ChangingTitleExampleInterface : RegistrableInterface {

    override val subcommand: String = "changing-title"

    override fun create(): Interface<*, *> = buildCombinedInterface {
        rows = 1

        val numberProperty = interfaceProperty(0)
        var number by numberProperty

        withTransform(numberProperty) { pane, view ->
            view.title(Component.text(number))

            val item = ItemStack(Material.STICK)
                .name("number -> $number")

            pane[0, 4] = StaticElement(Drawable.drawable(item)) {
                number += 1
            }
        }
    }
}
