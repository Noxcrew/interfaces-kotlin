package com.noxcrew.interfaces.grid

/** Generates a set of [GridPoint]'s. */
public fun interface GridPositionGenerator {

    /** Returns a list of [GridPoint]'s. */
    public fun generate(): List<GridPoint>
}
