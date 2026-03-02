package com.noxcrew.interfaces.example

import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.interfaces.Interface
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.utilities.forEachInGrid
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

public class ModifableAreaExampleInterface : RegistrableInterface {
    override val subcommand: String = "modifiable-area"

    override fun create(): Interface<*, *> = buildChestInterface {
        rows = 6
        preventClickingEmptySlots = false

        // Drop items that are placed in the modifiable slots!
        returnPlacedIntoInventoryOnClose()

        val usableSlots = mutableListOf<GridPoint>()
        for (x in 1..3) {
            for (y in 3..5) {
                usableSlots += GridPoint(x, y)
            }
        }

        withTransform { pane, _ ->
            forEachInGrid(5, 9) { row, column ->
                val item = ItemStack(Material.DIRT)
                    .name("magical circle of dirt that allows modification")

                pane[row, column] = StaticElement(drawable(item)) { (player) ->
                    player.sendMessage("its dirt!")
                }
            }

            for (slot in usableSlots) {
                pane[slot] = StaticElement(Drawable.empty(), isSlotModifiable = true)
            }
        }
    }
}
