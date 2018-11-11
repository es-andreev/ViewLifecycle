package com.ea.viewlifecycle

import android.view.View
import android.view.ViewGroup

internal class HierarchyLifecycleDispatcher(private val rootView: ViewGroup) : LifecycleDispatcher(rootView) {

    private var viewGroups: ArrayList<ViewGroup> = arrayListOf()

    private val viewGroupComparator = ViewGroupComparator()

    private val hierarchyChangeListener = object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewRemoved(parent: View?, child: View?) {
            dispatchLifecycleOnLayout()
        }

        override fun onChildViewAdded(parent: View?, child: View?) {
            dispatchLifecycleOnLayout()
        }
    }

    internal fun addViewGroup(viewGroup: ViewGroup) {
        if (viewGroups.contains(viewGroup)) {
            return
        }

        viewGroups.add(viewGroup)

        if (viewGroups.size > 1) {
            if (viewGroups.size == 2) {
                rootView.lifecycleOwner
                rootView.hierarchyLifecycleDispatcher = this

                viewGroups[0].setOnHierarchyChangeListener(hierarchyChangeListener)
            }

            viewGroup.setOnHierarchyChangeListener(hierarchyChangeListener)
            dispatchLifecycleOnLayout()
        }
    }

    internal fun removeViewGroup(viewGroup: ViewGroup) {
        if (!viewGroups.contains(viewGroup)) {
            return
        }

        viewGroups.remove(viewGroup)
        viewGroup.setOnHierarchyChangeListener(null)

        @Suppress("CascadeIf")
        if (viewGroups.size > 1) {
            dispatchLifecycleOnLayout()
        } else if (viewGroups.size == 1) {
            viewGroups[0].setOnHierarchyChangeListener(null)

            rootView.hierarchyLifecycleDispatcher = null
        }
    }

    override fun detach() {
        super.detach()
        viewGroups.forEach { it.setOnHierarchyChangeListener(null) }
        viewGroups.clear()
        rootView.hierarchyLifecycleDispatcher = null
    }

    override fun getZSortedViews(): Array<View> {
        viewGroups.sortWith(viewGroupComparator)
        return viewGroups.toTypedArray()
    }

    /**
     * Sorts ViewGroups in the following order:
     * - by visibility;
     * - by children presence;
     * - by order in the view tree.
     */
    private class ViewGroupComparator : Comparator<ViewGroup> {
        override fun compare(v1: ViewGroup, v2: ViewGroup): Int {
            val v1Displayed = v1.isDisplayed
            val v2Displayed = v2.isDisplayed

            return if (v2Displayed && !v1Displayed) {
                1
            } else if (!v2Displayed && v1Displayed) {
                -1
            } else {
                val v1HasViews = v1.childCount > 0
                val v2HasViews = v2.childCount > 0

                return if (v2HasViews && !v1HasViews) {
                    1
                } else if (!v2HasViews && v1HasViews) {
                    -1
                } else {
                    // TODO hierarchy position sort
                    -1
                }
            }
        }
    }
}