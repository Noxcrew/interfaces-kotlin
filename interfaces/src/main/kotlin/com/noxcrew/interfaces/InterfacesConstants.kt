package com.noxcrew.interfaces

import com.noxcrew.interfaces.utilities.InterfacesCoroutineDetails
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.bukkit.Bukkit
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Holds the shared scope used for any interfaces coroutines. */
public object InterfacesConstants {

    private val EXCEPTION_LOGGER = LoggerFactory.getLogger("InterfacesExceptionHandler")

    /** The [CoroutineScope] for any suspending operations performed by interfaces. */
    public val SCOPE: CoroutineScope = CoroutineScope(
        CoroutineName("interfaces") +
            SupervisorJob() +
            CoroutineExceptionHandler { context, exception ->
                val details = context[InterfacesCoroutineDetails]

                if (details == null) {
                    EXCEPTION_LOGGER.error("An unknown error occurred in a coroutine!", exception)
                } else {
                    val (player, reason) = details
                    EXCEPTION_LOGGER.error(
                        """
                        An unknown error occurred in a coroutine!
                         - Player: ${player ?: "N/A"} (${player?.let(Bukkit::getPlayer)?.name ?: "offline"})
                         - Launch reason: $reason
                        """.trimIndent(),
                        exception,
                    )
                }
            } +
            run {
                val threadNumber = AtomicInteger()
                val factory = { runnable: Runnable ->
                    Thread("interfaces-${threadNumber.getAndIncrement()}").apply {
                        isDaemon = true
                    }
                }

                System.getProperty("com.noxcrew.interfaces.fixedPoolSize")
                    ?.toIntOrNull()
                    ?.coerceAtLeast(2)
                    ?.let { size ->
                        if (System.getProperty("com.noxcrew.interfaces.useScheduledPool").toBoolean()) {
                            Executors.newScheduledThreadPool(size, factory)
                        } else {
                            Executors.newFixedThreadPool(size, factory)
                        }
                    }
                    ?.asCoroutineDispatcher()
                    ?: Dispatchers.Default
            },
    )
}
