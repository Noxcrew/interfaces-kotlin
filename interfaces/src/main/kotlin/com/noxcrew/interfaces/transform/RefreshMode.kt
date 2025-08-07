package com.noxcrew.interfaces.transform

/** The different modes for when a transform is refreshed. */
public enum class RefreshMode {
    /** The transform is refreshed only when one of its triggers is triggered. */
    TRIGGER_ONLY,

    /** The transform is refreshed only when a menu is opened for the first time. */
    INITIAL,

    /** The transform is refreshed any time the menu is opened. */
    ALWAYS,

    /** The transform is refreshed only when opened with reload = true or when the menu is opened for the first time. */
    RELOAD,
}
