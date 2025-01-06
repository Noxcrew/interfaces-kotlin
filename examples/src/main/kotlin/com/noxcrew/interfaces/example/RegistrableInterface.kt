package com.noxcrew.interfaces.example

import com.noxcrew.interfaces.interfaces.Interface

public interface RegistrableInterface {

    public val subcommand: String

    public fun create(): Interface<*, *>
}
