package com.noxcrew.interfaces.drawable

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** A drawable is a function that takes in a [Player] and creates an [ItemStack] to show that player. */
public fun interface Drawable {
    public companion object {
        /** Creates a new [Drawable] out of a given [item]. */
        public fun drawable(item: ItemStack): Drawable = Drawable { item }

        /** Creates a new [Drawable] out of a given [material]. */
        public fun drawable(material: Material): Drawable = Drawable { ItemStack(material) }
    }

    /** Draws this drawable for [player], creating an item stack. */
    public suspend fun draw(player: Player): ItemStack
}
