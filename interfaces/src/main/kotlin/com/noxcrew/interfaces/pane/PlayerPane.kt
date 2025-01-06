package com.noxcrew.interfaces.pane

import com.noxcrew.interfaces.element.Element
import com.noxcrew.interfaces.grid.GridPoint

/** An ordered pane that wraps the player inventory. */
public class PlayerPane : OrderedPane(PANE_ORDERING) {
    internal companion object {
        /** The base ordering of the player inventory to go from logical rows to Bukkit rows. */
        internal val PANE_ORDERING = listOf(1, 2, 3, 0, 4)

        /** The location of the off-hand slot. */
        internal val OFF_HAND_SLOT = GridPoint.at(4, 4)
    }

    /** The hotbar of the player inventory. */
    public val hotbar: Hotbar = Hotbar()

    /** The armor slots of the player inventory. */
    public val armor: Armor = Armor()

    /** The off hand slot of the player inventory. */
    public var offHand: Element
        get() = get(OFF_HAND_SLOT) ?: Element.EMPTY
        set(value) = set(OFF_HAND_SLOT, value)

    public inner class Hotbar {
        /** Returns the item in the hotbar at the [slot]-th slot. */
        public operator fun get(slot: Int): Element? = get(3, slot)

        /** Sets the item in the hotbar at the [slot]-th slot to [value]. */
        public operator fun set(
            slot: Int,
            value: Element,
        ): Unit = set(3, slot, value)
    }

    public inner class Armor {
        /** The helmet slot of the player's armor items. */
        public var helmet: Element
            get() = get(4, 3) ?: Element.EMPTY
            set(value) = set(4, 3, value)

        /** The chest slot of the player's armor items. */
        public var chest: Element
            get() = get(4, 2) ?: Element.EMPTY
            set(value) = set(4, 2, value)

        /** The leggings slot of the player's armor items. */
        public var leggings: Element
            get() = get(4, 1) ?: Element.EMPTY
            set(value) = set(4, 1, value)

        /** The boots slot of the player's armor items. */
        public var boots: Element
            get() = get(4, 0) ?: Element.EMPTY
            set(value) = set(4, 0, value)
    }
}
