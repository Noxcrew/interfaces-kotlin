package com.noxcrew.interfaces.grid

/** Generates a set of [GridPoint]'s. */
public fun interface GridPositionGenerator : Iterable<GridPoint> {

    /** Returns a list of [GridPoint]'s. */
    public fun generate(): List<GridPoint>

    override fun iterator(): Iterator<GridPoint> {
        return generate().iterator()
    }
}
