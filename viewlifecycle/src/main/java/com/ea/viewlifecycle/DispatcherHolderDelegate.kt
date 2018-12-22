package com.ea.viewlifecycle

import android.view.View
import android.view.ViewGroup
import kotlin.reflect.KProperty

/**
 * Holds views with [ViewGroupLifecycleDispatcher] attached.
 */
internal class DispatcherHolderDelegate : HolderDelegate<ViewGroupLifecycleDispatcher>() {

    override operator fun setValue(thisRef: View, property: KProperty<*>, value: ViewGroupLifecycleDispatcher?) {
        thisRef as? ViewGroup
                ?: throw IllegalStateException("Only ViewGroups can have ViewGroupLifecycleDispatcher.")

        val root = thisRef.safeRoot
                ?: throw IllegalStateException("View is not attached to a parent.")

        val prevCount = values.size
        super.setValue(thisRef, property, value)
        val currentCount = values.size

        if (prevCount == 0 && currentCount == 1) {
            root.attachHierarchyLifecycleDispatcher()
        }

        if (value == null) {
            root.hierarchyLifecycleDispatcher?.removeViewGroup(thisRef)
        } else {
            root.hierarchyLifecycleDispatcher?.addViewGroup(thisRef)
        }

        if (prevCount == 1 && currentCount == 0) {
            root.detachHierarchyLifecycleDispatcher()
        }
    }
}