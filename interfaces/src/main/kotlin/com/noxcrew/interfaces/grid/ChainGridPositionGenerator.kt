package com.noxcrew.interfaces.grid

/** A grid position generator that chains together two other generators. */
public data class ChainGridPositionGenerator(
    /** The first generator. */
    private val first: GridPositionGenerator,
    /** The second generator. */
    private val second: GridPositionGenerator,
) : GridPositionGenerator {
    public companion object {
        /** Adds two grid position generators together. */
        public operator fun GridPositionGenerator.plus(second: GridPositionGenerator): ChainGridPositionGenerator =
            ChainGridPositionGenerator(this, second)
    }

    override fun generate(): List<GridPoint> = first.generate() + second.generate()
}
