package com.noxcrew.interfaces.exception

import com.noxcrew.interfaces.InterfacesListeners
import net.kyori.adventure.text.Component
import org.slf4j.LoggerFactory

/** The standard implementation that handles an interfaces exception. */
public open class StandardInterfacesExceptionHandler(
    /** Whether decorations are allowed to fail gracefully. */
    public val allowDecorationFailure: Boolean = true,
) : InterfacesExceptionHandler {
    private val logger = LoggerFactory.getLogger(StandardInterfacesExceptionHandler::class.java)

    override suspend fun handleException(exception: Exception, context: InterfacesExceptionContext): InterfacesExceptionResolution {
        // Optionally gracefully let decorations fail without issue! This defaults to true because we also
        // allow items to be clicked without their decorations.
        if (allowDecorationFailure && context.operation == InterfacesOperation.DECORATING_ELEMENT) {
            logger.warn("Failed to decorate interface elements for ${context.player.name}")
            return InterfacesExceptionResolution.IGNORE
        }

        return when (context.operation) {
            InterfacesOperation.BUILDING_PLAYER -> {
                if (context.retries < 3) {
                    // When building a player menu we absolutely do not want to fail as
                    // it results in nothing being drawn. We retry up to 3 times!
                    logger.error("Failed to build player inventory for ${context.player.name}, retrying (${context.retries + 1}/3)!")
                    InterfacesExceptionResolution.RETRY
                } else {
                    // Ignore the drawing but kick the player off the server
                    logger.error(
                        "Failed to build player inventory for ${context.player.name} 3 times, kicking player to prevent invalid state",
                    )
                    InterfacesListeners.INSTANCE.runSync {
                        context.player.kick(Component.text("Unknown exception occurred while rendering GUI menus"))
                    }
                    InterfacesExceptionResolution.IGNORE
                }
            }

            else -> {
                // In general, we log an error and then close the menu!
                logger.error(
                    "An error occurred while performing interface operation `${context.operation.description}` for ${context.player.name}",
                    exception,
                )
                InterfacesExceptionResolution.CLOSE
            }
        }
    }
}
