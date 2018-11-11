package com.ea.viewlifecycle

import android.view.View
import android.view.ViewGroup
import kotlin.reflect.KProperty

/**
 * Holds views with [ViewLifecycleDispatcher] attached.
 */
internal class DispatcherHolderDelegate : HolderDelegate<ViewLifecycleDispatcher>() {

    private var hierarchyLifecycleDispatcher: HierarchyLifecycleDispatcher? = null

    override operator fun setValue(thisRef: View, property: KProperty<*>, value: ViewLifecycleDispatcher?) {
        if (thisRef !is ViewGroup) {
            throw IllegalStateException("Only ViewGroups can have ViewLifecycleDispatcher.")
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