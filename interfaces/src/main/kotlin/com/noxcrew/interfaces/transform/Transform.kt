package com.noxcrew.interfaces.transform

import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.view.InterfaceView

/** A transform which edits (transforms) a page in a view. */
public fun interface Transform<P : Pane> {
    // Defined differently to work around KT-74673: https://youtrack.jetbrains.com/issue/KT-74673/K2-ClassCastException-when-passing-suspending-functional-interface-with-generic
    public suspend operator fun invoke(pane: P, view: InterfaceView)
}
