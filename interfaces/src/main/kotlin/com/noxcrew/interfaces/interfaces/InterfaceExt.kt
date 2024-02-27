package com.noxcrew.interfaces.interfaces

/** Creates a new [ChestInterface] using a [ChestInterfaceBuilder]. */
public inline fun buildChestInterface(builder: ChestInterfaceBuilder.() -> Unit): ChestInterface =
    ChestInterfaceBuilder().also(builder).build()

/** Creates a new [PlayerInterface] using a [PlayerInterfaceBuilder]. */
public inline fun buildPlayerInterface(builder: PlayerInterfaceBuilder.() -> Unit): PlayerInterface =
    PlayerInterfaceBuilder().also(builder).build()

/** Creates a new [CombinedInterface] using a [CombinedInterfaceBuilder]. */
public inline fun buildCombinedInterface(builder: CombinedInterfaceBuilder.() -> Unit): CombinedInterface =
    CombinedInterfaceBuilder().also(builder).build()
