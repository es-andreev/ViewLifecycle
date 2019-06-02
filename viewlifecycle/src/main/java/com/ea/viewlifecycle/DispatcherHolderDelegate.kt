package com.ea.viewlifecycle

import android.view.ViewGroup
import kotlin.reflect.KProperty

/**
 * Holds views with [ViewGroupLifecycleDispatcher] attached.
 */
internal class DispatcherHolderDelegate : HolderDelegate<ViewGroup, ViewGroupLifecycleDispatcher>() {

    private var root: ViewGroup? = null

    override operator fun setValue(thisRef: ViewGroup,
                                   property: KProperty<*>,
                                   value: ViewGroupLifecycleDispatcher?) {
        val prevCount = values.size
        super.setValue(thisRef, property, value)
        val currentCount = values.size

        if (prevCount == 0 && currentCount == 1) {
            val safeRoot = thisRef.safeRoot
            root = safeRoot ?: throw IllegalStateException("View is not attached to a parent.")
            root?.attachHierarchyLifecycleDispatcher()
        }

        if (value == null) {
            root?.hierarchyLifecycleDispatcher?.removeViewGroup(thisRef)
        } else {
            root?.hierarchyLifecycleDispatcher?.addViewGroup(thisRef)
        }

        if (prevCount == 1 && currentCount == 0) {
            root?.detachHierarchyLifecycleDispatcher()
            root = null
        }
    }
}