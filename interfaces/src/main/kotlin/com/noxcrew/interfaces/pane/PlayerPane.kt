package com.noxcrew.interfaces.pane

import com.noxcrew.interfaces.element.Element
import com.noxcrew.interfaces.grid.GridPoint

/** An ordered pane that wraps the player inventory. */
public class PlayerPane : Pane() {

    internal companion object {
        /** The row used for the hot bar slots. */
        internal const val HOT_BAR_ROW = 3

        /** The row used for armor and crafting slots. */
        internal const val EXTRA_ROW = 4

        /** The row used for the offhand slot. */
        internal const val OFFHAND_ROW = 5

        /** The base ordering of the player inventory to go from logical rows to Bukkit rows. */
        internal val PANE_ORDERING = listOf(1, 2, 3, 0, 4)

        /** The location of the off-hand slot. */
        internal val OFF_HAND_SLOT = GridPoint.at(OFFHAND_ROW, 0)

        internal val HELMET_SLOT = GridPoint.at(EXTRA_ROW, 5)
        internal val CHEST_SLOT = GridPoint.at(EXTRA_ROW, 6)
        internal val LEGGING_SLOT = GridPoint.at(EXTRA_ROW, 7)
        internal val BOOTS_SLOT = GridPoint.at(EXTRA_ROW, 8)
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
        public operator fun get(slot: Int): Element? = get(HOT_BAR_ROW, slot)

        /** Sets the item in the hotbar at the [slot]-th slot to [value]. */
        public operator fun set(slot: Int, value: Element): Unit = set(HOT_BAR_ROW, slot, value)
    }

    public inner class Armor {
        /** The helmet slot of the player's armor items. */
        public var helmet: Element
            get() = get(HELMET_SLOT) ?: Element.EMPTY
            set(value) = set(HELMET_SLOT, value)

        /** The chest slot of the player's armor items. */
        public var chest: Element
            get() = get(CHEST_SLOT) ?: Element.EMPTY
            set(value) = set(CHEST_SLOT, value)

        /** The legging slot of the player's armor items. */
        public var leggings: Element
            get() = get(LEGGING_SLOT) ?: Element.EMPTY
            set(value) = set(LEGGING_SLOT, value)

        /** The boots slot of the player's armor items. */
        public var boots: Element
            get() = get(BOOTS_SLOT) ?: Element.EMPTY
            set(value) = set(BOOTS_SLOT, value)
    }
}
