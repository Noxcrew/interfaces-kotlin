package com.noxcrew.interfaces.grid

/** Stores elements of type [V] at a row and column coordinate in a 2d space. */
public interface GridMap<V> {
    /** Sets the value at ([row], [column]) to [value]. */
    public operator fun set(
        row: Int,
        column: Int,
        value: V,
    )

    /** Sets the value at ([vector]) to [value]. */
    public operator fun set(
        vector: GridPoint,
        value: V,
    ) {
        set(vector.x, vector.y, value)
    }

    /** Returns the value at ([row], [column]), or `null` if none exists at the given location. */
    public operator fun get(
        row: Int,
        column: Int,
    ): V?

    /** Returns the value at ([vector]), or `null` if none exists at the given location. */
    public operator fun get(vector: GridPoint): V? = get(vector.x, vector.y)

    /** Returns whether this map contains a value at ([row], [column]). */
    public fun has(
        row: Int,
        column: Int,
    ): Boolean

    /** Returns whether this map contains a value at ([vector]). */
    public fun has(vector: GridPoint): Boolean = has(vector.x, vector.y)

    /** Runs [consumer] for each element in this map. */
    public fun forEach(consumer: (row: Int, column: Int, V) -> Unit)

    /** Runs suspending [consumer] for each element in this map. */
    public suspend fun forEachSuspending(consumer: suspend (row: Int, column: Int, V) -> Unit)
}
