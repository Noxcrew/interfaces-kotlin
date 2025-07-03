package com.noxcrew.interfaces.utilities

import com.noxcrew.interfaces.view.AbstractInterfaceView
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.WeakHashMap
import kotlin.time.toKotlinDuration

/** Assists in profiling interfaces. */
public object InterfacesProfiler {
    private val logger = LoggerFactory.getLogger(InterfacesProfiler::class.java)

    private val lastTimes = WeakHashMap<AbstractInterfaceView<*, *, *>, Instant>()

    /** Logs the given [action] for [view]. */
    public fun log(view: AbstractInterfaceView<*, *, *>, action: String) {
        if (!view.builder.debugRenderingTime) return

        val lastTime = lastTimes[view]
        val message = StringBuilder()
        message.append("Action `$action` has run for ${view.player.name}")
        if (lastTime != null) {
            val timeSinceLast = Duration.between(lastTime, Instant.now())
            message.append(", took ${timeSinceLast.toKotlinDuration()}")
        }
        logger.info(message.toString())
        lastTimes[view] = Instant.now()
    }

    /** Logs a general action that started at [start]. */
    public fun logAbstract(action: String, start: Instant) {
        val timeSinceLast = Duration.between(start, Instant.now())
        logger.info("Action `$action` has run, took ${timeSinceLast.toKotlinDuration()}")
    }
}