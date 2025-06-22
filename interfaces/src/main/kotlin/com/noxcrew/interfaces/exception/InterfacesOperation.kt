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

    CLOSE_HANDLER("running close handler"),
    MARK_CLOSED("marking inventory as closed"),

    CHAT_QUERY_CANCELLATION("cancelling chat query"),
    CHAT_QUERY_COMPLETION("completing chat query"),

    RUNNING_CLICK_HANDLER("running click handler"),

    UPDATING_STATE("updating state property"),
    UPDATING_LAZY("updating lazy property"),
    UPDATING_PROPERTIES("updating trigger properties"),
}
