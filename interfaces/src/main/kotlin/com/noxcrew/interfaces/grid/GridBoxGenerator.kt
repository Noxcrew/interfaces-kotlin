package com.noxcrew.interfaces.grid

/**
 * A grid position generator that generates every point inside a box with corners of
 * [min] and [max] (inclusive).
 */
public data class GridBoxGenerator(
    private val min: GridPoint,
    private val max: GridPoint
) : GridPositionGenerator {

    public constructor(x1: Int, y1: Int, x2: Int, y2: Int) : this(GridPoint(x1, y1), GridPoint(x2, y2))

    override fun iterator(): Iterator<GridPoint> =
        (min..max).iterator()

    override fun generate(): List<GridPoint> =
        (min..max).toList()
}
