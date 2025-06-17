package com.noxcrew.interfaces.exception

/** The types of interfaces operations that can throw exceptions. */
public enum class InterfacesOperation(
    /** The user-friendly description of the type of operation that encountered the exception. */
    public val description: String,
) {
    BUILDING_PLAYER("building player interface"),
    BUILDING_REGULAR("building regular interface"),

    SYNC_DRAW_INVENTORY("drawing inventory on main thread"),
    RENDER_INVENTORY("rendering to inventory"),
    APPLY_TRANSFORM("applying transforms to interface"),
    DECORATING_ELEMENT("decorating inventory element"),

    UPDATING_STATE("updating state property"),
}
