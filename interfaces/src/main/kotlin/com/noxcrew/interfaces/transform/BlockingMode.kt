package com.noxcrew.interfaces.transform

/** The type of blocking a transform should perform. */
public enum class BlockingMode {
    /** The inventory is always drawn before applying this transform. */
    NONE,

    /** The inventory is initially blocked for this transform, but not when re-opening. */
    INITIAL,

    /** The inventory always has to block for this transform to re-apply. */
    ALWAYS,
}
