package com.noxcrew.interfaces.grid

/** A 2-dimensional vector storing integer components. */
public data class GridPoint(val x: Int, val y: Int) {

    public companion object {
        /** The possible valid slot range inside the player inventory. */
        public val PLAYER_INVENTORY_RANGE: IntRange = 0..40

        /** The slot index used to indicate a click was outside the UI. */
        public const val OUTSIDE_CHEST_INDEX: Int = -999

        private val OLD_PANE_MAPPING = listOf(1, 2, 3, 0, 4)
        /** Returns the grid point for a [slot] in a player inventory. */
        public fun fromBukkitPlayerSlot(slot: Int): GridPoint? {
            if (slot !in PLAYER_INVENTORY_RANGE) return null
            val x = slot / 9
            val adjustedX = OLD_PANE_MAPPING.indexOf(x)
            return GridPoint(adjustedX, slot % 9)
        }

        /** Returns the grid point for a [slot] in a chest inventory. */
        public fun fromBukkitChestSlot(slot: Int): GridPoint? {
            if (slot == OUTSIDE_CHEST_INDEX) return null
            return GridPoint(slot / 9, slot % 9)
        }

        /** Creates a new [GridPoint] for ([x], [y]). */
        public fun at(x: Int, y: Int): GridPoint = GridPoint(x, y)
    }

    /** Returns a new [GridPoint] equal to this + [other]. */
    public operator fun plus(other: GridPoint): GridPoint = GridPoint(x + other.x, y + other.y)

    /** Returns a new [GridPoint] equal to this - [other]. */
    public operator fun minus(other: GridPoint): GridPoint = GridPoint(x - other.x, y - other.y)

    /** Returns a sequence of grid points between this and [other]. */
    public operator fun rangeTo(other: GridPoint): Sequence<GridPoint> = sequence {
        for (x in x..other.x) {
            for (y in y..other.y) {
                yield(at(x, y))
            }
        }
    }
}
