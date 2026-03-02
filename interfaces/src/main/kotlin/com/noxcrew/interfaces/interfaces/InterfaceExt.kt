package com.noxcrew.interfaces.interfaces

/** Creates a new interface using a [ContainerInterfaceBuilder.Simple]. */
public inline fun buildChestInterface(builder: ContainerInterfaceBuilder.Simple.() -> Unit): ContainerInterface.Simple =
    ContainerInterfaceBuilder.Simple().also(builder).build()

/** Creates a new [PlayerInterface] using a [PlayerInterfaceBuilder]. */
public inline fun buildPlayerInterface(builder: PlayerInterfaceBuilder.() -> Unit): PlayerInterface =
    PlayerInterfaceBuilder().also(builder).build()

/** Creates a new [ContainerInterface] using a [ContainerInterfaceBuilder.Simple] with an [PlayerInventoryType.OVERRIDE] type player inventory. */
public inline fun buildCombinedInterface(builder: ContainerInterfaceBuilder.Simple.() -> Unit): ContainerInterface.Simple =
    ContainerInterfaceBuilder.Simple().also {
        it.playerInventoryType = PlayerInventoryType.OVERRIDE
    }.also(builder).build()
