package com.noxcrew.interfaces.transform

import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.properties.InterfaceProperty

/**
 * A transform whose state solely relies on a singular property
 * where each distinct state of the property should be persisted
 * as it is possibly re-used within the lifetime of the menu.
 *
 * This is useful when the transform is lazy or contains lazy
 * elements to prevent re-decorating when switching this transform.
 *
 * The transform is not redrawn if property changes to the same
 * value it previously had, the old state is re-used similar to
 * re-opening the menu.
 */
public interface StatefulTransform<P : Pane, T> : ReactiveTransform<P> {

    /**
     * The property this transform where each distinct value
     * is cached. This property should NEVER be used to trigger
     * a redraw of the menu.
     */
    public val property: InterfaceProperty<T>
}
