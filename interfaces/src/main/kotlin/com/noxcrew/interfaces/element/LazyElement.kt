package com.noxcrew.interfaces.element

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** An element that lazily updates its item stack later based on a suspending call. */
public interface LazyElement : Element {

    /** Requires that the decoration function finishes before the click handler will work. */
    public val requireDecorationToClick: Boolean
        get() = false

    /**
     * Decorates the given [itemStack] for [player] lazily.
     * The item is already drawn to the interface while this call is running. Can be used
     * to lazily add decorations such as filling out lore text or editing the item model.
     */
    public suspend fun decorate(player: Player, itemStack: ItemStack)
}
