package com.noxcrew.interfaces.transform

import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.view.InterfaceView

public fun interface Transform<P : Pane> : suspend (P, InterfaceView) -> Unit
