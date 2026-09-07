package com.noxcrew.interfaces.element

import com.noxcrew.interfaces.properties.Trigger
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** An element that lazily updates its item stack later based on a suspending call. */
public interface LazyElement : Element {

    /** Requires that the decoration function finishes before the click handler will work. */
    public val requireDecorationToClick: Boolean
        get() = false

    /** Returns a list of triggers to re-decorate this element for. */
    public suspend fun getRedecorationTriggers(player: Player): List<Trigger> = emptyList()

    /**
     * Decorates the given [itemStack] for [player] lazily.
     * The item is already drawn to the interface while this call is running. Can be used
     * to lazily add decorations such as filling out lore text or editing the item model.
     */
    public suspend fun decorate(player: Player, itemStack: ItemStack)
}
