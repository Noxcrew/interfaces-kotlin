package com.noxcrew.interfaces.example

import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.buildChestInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import kotlin.time.Duration.Companion.seconds

public class DelayedRequestExampleInterface : RegistrableInterface {

    private companion object {
        private val BACKING_ELEMENT = StaticElement(Drawable.drawable(Material.GRAY_CONCRETE))
    }

    override val subcommand: String = "delayed"

    @OptIn(DelicateCoroutinesApi::class)
    override fun create(): Interface<*, *> = buildChestInterface {
        titleSupplier = { text(subcommand) }
        rows = 2

        withTransform { pane, _ ->
            suspendingData().forEachIndexed { index, material ->
                pane[0, index] = StaticElement(Drawable.drawable(material))
            }
        }

        withTransform { pane, _ ->
            for (index in 0..8) {
                pane[1, index] = BACKING_ELEMENT
            }

            pane[0, 8] = StaticElement(Drawable.drawable(Material.ENDER_PEARL)) {
                // This is very unsafe, it's up to you to set up a way to reliably
                // launch coroutines per player in a click handler.
                GlobalScope.launch {
                    it.view.back()
                }
            }
        }
    }

    private suspend fun suspendingData(): List<Material> {
        delay(3.seconds)
        return listOf(Material.GREEN_CONCRETE, Material.YELLOW_CONCRETE, Material.RED_CONCRETE)
    }
}
