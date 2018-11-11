package com.ea.viewlifecycle

import android.view.View
import android.view.ViewGroup
import kotlin.reflect.KProperty

/**
 * Holds views with [ViewGroupLifecycleDispatcher] attached.
 */
internal class DispatcherHolderDelegate : HolderDelegate<ViewGroupLifecycleDispatcher>() {

    private var hierarchyLifecycleDispatcher: HierarchyLifecycleDispatcher? = null

    override operator fun setValue(thisRef: View, property: KProperty<*>, value: ViewGroupLifecycleDispatcher?) {
        if (thisRef !is ViewGroup) {
            throw IllegalStateException("Only ViewGroups can have ViewGroupLifecycleDispatcher.")
        }

        val prevCount = values.size
        super.setValue(thisRef, property, value)
        val currentCount = values.size

        if (prevCount == 0 && currentCount == 1) {
            hierarchyLifecycleDispatcher = HierarchyLifecycleDispatcher(thisRef.root)
        }

        if (value == null) {
            hierarchyLifecycleDispatcher?.removeViewGroup(thisRef)
        } else {
            hierarchyLifecycleDispatcher?.addViewGroup(thisRef)
        }

        if (prevCount == 1 && currentCount == 0) {
            hierarchyLifecycleDispatcher = null
        }
    }
}