package com.noxcrew.interfaces.example

import com.noxcrew.interfaces.InterfacesListeners
import com.noxcrew.interfaces.drawable.Drawable.Companion.drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.interfaces.buildCombinedInterface
import com.noxcrew.interfaces.interfaces.buildPlayerInterface
import com.noxcrew.interfaces.properties.interfaceProperty
import com.noxcrew.interfaces.utilities.forEachInGrid
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.kotlin.extension.buildAndRegister
import org.incendo.cloud.paper.LegacyPaperCommandManager

public class ExamplePlugin : JavaPlugin(), Listener {

    private companion object {
        private val INTERFACES = listOf(
            DelayedRequestExampleInterface(),
            ChangingTitleExampleInterface(),
            CatalogueExampleInterface(),
            MovingExampleInterface(),
            TabbedExampleInterface()
        )
    }

    private val counterProperty = interfaceProperty(5)
    private var counter by counterProperty

    override fun onEnable() {
        val commandManager = LegacyPaperCommandManager.createNative(this, ExecutionCoordinator.asyncCoordinator())
        commandManager.buildAndRegister("interfaces") {
            registerCopy {
                literal("close")

                suspendingHandler {
                    val player = it.sender() as Player
                    InterfacesListeners.INSTANCE.getOpenInterface(player.uniqueId)?.close()
                    player.inventory.clear()
                }
            }

            registerCopy {
                literal("simple")

                suspendingHandler {
                    val player = it.sender() as Player
                    val simpleInterface = simpleInterface()

                    simpleInterface.open(player)
                }
            }

            registerCopy {
                literal("combined")

                suspendingHandler {
                    val player = it.sender() as Player
                    val combinedInterface = combinedInterface()

                    combinedInterface.open(player)
                }
            }

            for (registrableInterface in INTERFACES) {
                registerCopy {
                    literal(registrableInterface.subcommand)

                    suspendingHandler {
                        val player = it.sender() as Player
                        val builtInterface = registrableInterface.create()

                        builtInterface.open(player)
                    }
                }
            }
        }

        InterfacesListeners.install(this)

        this.server.pluginManager.registerEvents(this, this)

        Bukkit.getScheduler().runTaskTimerAsynchronously(
            this,
            Runnable {
                counter++
            },
            0,
            1
        )
    }

    @EventHandler
    public fun onJoin(e: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskAsynchronously(
            this,
            Runnable {
                runBlocking {
                    playerInterface().open(e.player)
                }
            }
        )
    }

    private fun simpleInterface() = buildChestInterface {
        rows = 6

        withTransform(counterProperty) { pane, _ ->
            val item = ItemStack(Material.BEE_NEST)
                .name("it's been $counter's ticks")
                .description("click to see the ticks now")

            pane[3, 3] = StaticElement(drawable(item)) {
                it.player.sendMessage("it's been $counter's ticks")
            }
        }

        withTransform { pane, _ ->
            val item = ItemStack(Material.BEE_NEST)
                .name("block the interface")
                .description("block interaction and message in 5 seconds")

            pane[5, 3] = StaticElement(drawable(item)) {
                completingLater = true

                runAsync(5) {
                    it.player.sendMessage("after blocking, it has been $counter's ticks")
                    complete()
                }
            }
        }

        withTransform { pane, _ ->
            forEachInGrid(6, 9) { row, column ->
                if (pane.has(row, column)) return@forEachInGrid

                val item = ItemStack(Material.WHITE_STAINED_GLASS_PANE)
                    .name("row: $row, column: $column")

                pane[row, column] = StaticElement(drawable(item))
            }
        }
    }

    private fun playerInterface() = buildPlayerInterface {
        // Use modern logic to only cancel the item interaction and not block interactions while
        // using this interface
        onlyCancelItemInteraction = true

        // Prioritise block interactions!
        prioritiseBlockInteractions = true

        withTransform { pane, _ ->
            val item = ItemStack(Material.COMPASS).name("interfaces example")

            pane.hotbar[3] = StaticElement(drawable(item)) { (player) ->
                player.sendMessage("hello")
            }

            pane.offHand = StaticElement(drawable(item)) { (player) ->
                player.sendMessage("hey")
            }

            val armor = ItemStack(Material.STICK)

            pane.armor.helmet = StaticElement(drawable(armor.name("helmet").clone()))

            pane.armor.chest = StaticElement(drawable(armor.name("chest").clone()))

            pane.armor.leggings = StaticElement(drawable(armor.name("leggings").clone()))

            pane.armor.boots = StaticElement(drawable(armor.name("boots").clone()))
        }
    }

    private fun combinedInterface() = buildCombinedInterface {
        rows = 6

        withTransform { pane, _ ->
            forEachInGrid(10, 9) { row, column ->
                val item = ItemStack(Material.WHITE_STAINED_GLASS_PANE)
                    .name("row: $row, column: $column")

                pane[row, column] = StaticElement(drawable(item)) { (player) ->
                    player.sendMessage("row: $row, column: $column")
                }
            }
        }
    }

    private fun runAsync(delay: Int, runnable: Runnable) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, runnable, delay * 20L)
    }
}
