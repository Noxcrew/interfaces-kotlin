package com.noxcrew.interfaces.exception

/** The different methods to handle the resolution of an exception. */
public enum class InterfacesExceptionResolution {
    /** The exception is entirely ignored and the method simply fails. */
    IGNORE,

    /** If possible, whatever logic was attempted is retried. */
    RETRY,

    /** The menu is closed. A background interface is re-opened if applicable. */
    CLOSE,
}
