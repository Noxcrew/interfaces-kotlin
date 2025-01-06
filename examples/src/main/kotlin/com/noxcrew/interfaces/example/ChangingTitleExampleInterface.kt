package com.noxcrew.interfaces.example

import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.properties.interfaceProperty
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

public class ChangingTitleExampleInterface : RegistrableInterface {

    override val subcommand: String = "changing-title"

    override fun create(): Interface<*, *> = buildChestInterface {
        rows = 1

        // Allow clicking the player inventory but not anything in the top inventory
        allowClickingOwnInventoryIfClickingEmptySlotsIsPrevented = true

        val numberProperty = interfaceProperty(0)
        var number by numberProperty

        withTransform(numberProperty) { pane, view ->
            view.title(Component.text(number))

            val item = ItemStack(Material.STICK)
                .name("number -> $number")

            pane[0, 4] = StaticElement(drawable(item)) {
                number += 1
            }

            pane[0, 6] = StaticElement(drawable(Material.ACACIA_SIGN))
        }
    }
}
