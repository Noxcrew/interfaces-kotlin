package com.noxcrew.interfaces.interfaces

/** The different types of handling of player inventories. */
public enum class PlayerInventoryType(
    /** Whether this types includes the slots in the player inventory. */
    public val includesPlayerInventory: Boolean,
) {
    /** The player's real inventory is shown. */
    DEFAULT(false),

    /** A fake copy of a player inventory is created. */
    FAKE(true),

    /** The player's inventory is overridden. */
    OVERRIDE(true),
}
