package com.noxcrew.interfaces.transform

/** The different types of transforms, decides how this type handles exceptions or slow loading. */
public enum class TransformType(
    /** Whether the menu should close when this transform produces an exception. */
    public val closeOnError: Boolean,
    /** Whether this transform delays the first render of the menu. */
    public val delayRender: Boolean,
) {
    /** For regular transforms which have to be completed and that need to be successful. */
    REGULAR(true, true),

    /** For transforms that are critical but do not block the interface. */
    LAZY(true, false),

    /** For transforms that do not delay rendering or cause exceptions. */
    OPTIONAL(false, false),

    /** For transforms that need to complete before rendering but that are ignored if they cause errors. */
    ERROR_PRONE(false, true),
}
