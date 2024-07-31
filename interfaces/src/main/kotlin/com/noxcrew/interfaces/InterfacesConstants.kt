package com.noxcrew.interfaces

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Holds the shared scope used for any interfaces coroutines. */
public object InterfacesConstants {

    /** The [CoroutineScope] for any suspending operations performed by interfaces. */
    public val SCOPE: CoroutineScope = CoroutineScope(
        CoroutineName("interfaces") +
            SupervisorJob() +
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
            }
    )
}
