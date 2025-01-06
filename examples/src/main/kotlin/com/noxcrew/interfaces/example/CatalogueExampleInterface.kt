package com.noxcrew.interfaces.example

import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.buildCombinedInterface
import kotlinx.coroutines.runBlocking
import org.bukkit.Material

public class CatalogueExampleInterface : RegistrableInterface {
    override val subcommand: String = "catalogue"

    override fun create(): Interface<*, *> = buildCombinedInterface {
        rows = 1

        withTransform { pane, _ ->
            pane[3, 3] = StaticElement(
                Drawable.drawable(Material.STICK)
            ) { (player) ->
                runBlocking {
                    ChangingTitleExampleInterface().create().open(player)
                }
            }
        }
    }
}
