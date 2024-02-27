package com.noxcrew.interfaces.grid

/** A 2-dimensional vector storing integer components. */
public data class GridPoint(val x: Int, val y: Int) {

    public companion object {
        /** Creates a new [GridPoint] for ([x], [y]). */
        public fun at(x: Int, y: Int): GridPoint = GridPoint(x, y)
    }

    /** Returns a new [GridPoint] equal to this + [other]. */
    public operator fun plus(other: GridPoint): GridPoint =
        GridPoint(x + other.x, y + other.y)

    /** Returns a new [GridPoint] equal to this - [other]. */
    public operator fun minus(other: GridPoint): GridPoint =
        GridPoint(x - other.x, y - other.y)

    /** Returns a sequence of grid points between this and [other]. */
    public operator fun rangeTo(other: GridPoint): Sequence<GridPoint> = sequence {
        for (x in x..other.x) {
            for (y in y..other.y) {
                yield(at(x, y))
            }
        }
    }
}
