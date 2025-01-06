package com.noxcrew.interfaces.pane

/** A combined pane where a given amount of chest rows are followed by the 4 player inventory rows. */
public class CombinedPane(
    public val chestRows: Int
) : OrderedPane(createMappings(chestRows)) {

    public companion object {
        /** Creates mappings for a combined view with [rows] rows. */
        public fun createMappings(rows: Int): List<Int> = buildList {
            IntRange(0, rows - 1).forEach(::add)

            // the players hotbar is row 0 in the players inventory,
            // for combined interfaces it makes more sense for hotbar
            // to be the last row, so reshuffle here.
            add(rows + 1)
            add(rows + 2)
            add(rows + 3)
            add(rows)
        }
    }
}
