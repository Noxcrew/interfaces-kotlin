package com.noxcrew.interfaces.utilities

import org.bukkit.Bukkit
import org.bukkit.plugin.java.PluginClassLoader

/** Runs [function] on the main thread. */
internal fun runSync(function: () -> Unit) {
    if (Bukkit.isPrimaryThread()) {
        function()
        return
    }

    val plugin = (function::class.java.classLoader as PluginClassLoader).plugin
    Bukkit.getScheduler().callSyncMethod(plugin!!, function)
}
