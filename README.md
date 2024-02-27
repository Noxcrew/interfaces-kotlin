# `interfaces-kotlin`

_Building interfaces in Kotlin since 2022._

`interfaces-kotlin` is a builder-style user interface library designed to make creation of flexible user interfaces as easy as 
possible.

This is a forked version of the [original interfaces by Incendo](https://github.com/Incendo/interfaces). Specifically, this 
repository contains a Kotlin version of interfaces v2 (or interfaces-next). The entire library is written in Kotlin and uses 
suspending methods for better coroutine support than what can be offered by a Java library.

## Terminology

#### Interface
An interface is the main class that you'll be interacting with. Interfaces hold a series of transformations and other values that
can be used to construct an InterfaceView.

#### InterfaceView ("view")
An InterfaceView represents a 'rendered' interface. An InterfaceView holds one pane and one InterfaceViewer.

#### Pane
A pane holds a collection of elements that make up the visual aspect of an interface.

#### Transform
A transformation ("transform") operates on a type of pane to add, remove, or change elements. Transformations are used to interact
with panes.

## Usage
Interfaces can be found on our [public Maven repository](https://maven.noxcrew.com/#/public/com/noxcrew/interfaces) and added to a Maven project as follows:

```xml
<repository>
    <id>noxcrew-maven</id>
    <name>Noxcrew Public Maven Repository</name>
    <url>https://maven.noxcrew.com/public</url>
</repository>

<dependency>
  <groupId>com.noxcrew.interfaces</groupId>
  <artifactId>interfaces</artifactId>
  <version>REPLACE_WITH_CURRENT_INTERFACES_VERSION</version>
</dependency>
```

## Example

<details open>
<summary>Creating an interface</summary>

This code creates a chest interface with 6 rows, a background fill, an ItemStack that counts how many ticks have passed and 
automatically updates itself, and an item that runs a coroutine for 5 seconds before allowing further click actions.

```kotlin
// Create a property that will trigger an update of the transform on change.
val counterProperty = interfaceProperty(5)
var counter by counterProperty

// Create a new chest interface. You can also create a player interface (player inventory) or combined interface (opened chest and player inventory below).
val chestInterface = buildChestInterface {
    rows = 6
    
    // Create a new transform that updates whenever the counter property changes
    withTransform(counterProperty) { pane, _ ->
        // Define the item to show. This transform is re-run whenever the property changes
        // so we can reference the variable here.
        val item = ItemStack(Material.BEE_NEST)
            .name("it's been $counter's ticks")
            .description("click to see the ticks now")

        // Create a new static element that runs the function when clicked.
        pane[3, 3] = StaticElement(drawable(item)) {
            it.player.sendMessage("it's been $counter's ticks")
        }
    }

    // Create a second transform that is only drawn once (on open) which has an item to run a delayed function
    withTransform { pane, _ ->
        val item = ItemStack(Material.BEE_NEST)
            .name("block the interface")
            .description("block interaction and message in 5 seconds")

        pane[5, 3] = StaticElement(drawable(item)) {
            // Indicate that we are going to run our own function and will run complete() later to resume usage of the menu
            completingLater = true

            // Use your own scheduling solution and/or coroutines to run a task, just make sure to call complete()!
            Bukkit.getScheduler().runTaskLaterAsynchronously(this, Runnable {
                it.player.sendMessage("after blocking, it has been $counter's ticks")
                complete()                                                  
            }, 100L)
        }
    }

    // Finally we draw a background by drawing a pane to each slot that is not already filled.
    withTransform { pane, _ ->
        forEachInGrid(6, 9) { row, column ->
            if (pane.has(row, column)) return@forEachInGrid

            val item = ItemStack(Material.WHITE_STAINED_GLASS_PANE)
                .name("row: $row, column: $column")

            pane[row, column] = StaticElement(drawable(item))
        }
    }
}
    
// We can easily open the interface which will return an InterfaceView instance
chestInterface.open(player)
```
</details>

Further examples can be found here: https://github.com/Noxcrew/interfaces/tree/main/examples

## Credits

Thanks to [broccolai](https://github.com/broccolai) for the original creation of Interfaces 2.

Thanks to [incendo](https://github.com/Incendo) for the creation of the [`interfaces` library](https://github.com/Incendo/interfaces).

Thanks to [kyori](https://github.com/kyoripowered) and their [`adventure` text library](https://github.com/kyoripowered/adventure).
