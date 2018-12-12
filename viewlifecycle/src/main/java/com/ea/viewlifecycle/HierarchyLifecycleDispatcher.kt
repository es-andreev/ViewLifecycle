package com.ea.viewlifecycle

import android.view.View
import android.view.ViewGroup
import java.util.*
import kotlin.collections.ArrayList

/**
 * A lifecycle dispatcher that handles multiple [ViewGroupLifecycleDispatcher]s.
 */
internal class HierarchyLifecycleDispatcher(private val rootView: ViewGroup) : LifecycleDispatcher(rootView) {

    private var viewGroups = hashSetOf<ViewGroup>()

    private val viewGroupComparator = ViewGroupComparator()

    private val hierarchyChangeListener = object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewRemoved(parent: View?, child: View?) {
            dispatchLifecycleOnLayout()
        }

        override fun onChildViewAdded(parent: View?, child: View?) {
            dispatchLifecycleOnLayout()
        }
    }

    init {
        rootView.hierarchyLifecycleDispatcher = this
        rootView.attachLifecycleOwner()
    }

    internal fun addViewGroup(viewGroup: ViewGroup) {
        if (viewGroups.add(viewGroup)) {
            viewGroup.setOnHierarchyChangeListener(hierarchyChangeListener)
            dispatchLifecycleOnLayout()
        }
    }

    internal fun removeViewGroup(viewGroup: ViewGroup) {
        if (viewGroups.remove(viewGroup)) {
            viewGroup.setOnHierarchyChangeListener(null)
            dispatchLifecycleOnLayout()
        }
    }

    override fun clear() {
        super.clear()
        viewGroups.forEach { it.setOnHierarchyChangeListener(null) }
        viewGroups.clear()
    }

    override fun getZSortedViews(): Collection<View> {
        return viewGroups.toSortedSet(viewGroupComparator)
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
                    val v1Level = v1.hierarchyLevel
                    val v2Level = v2.hierarchyLevel
                    return v2Level - v1Level
                }
            }
        }

        private val ViewGroup.hierarchyLevel: Int
            get() {
                val fullStem = ArrayList<ViewGroup>(20).apply {
                    add(this@hierarchyLevel)
                    addAll(this@hierarchyLevel.stem)
                }

                var parentMax = Int.MAX_VALUE
                var level = 0
                for (i in fullStem.size - 1 downTo 1) {
                    val parent = fullStem[i]
                    val child = fullStem[i - 1]
                    val index = parent.indexOfChild(child) + 1
                    if (index == 0) {
                        throw IllegalStateException("Wrong hierarchy state: $parent is not a parent of $child.")
                    }

                    val step = parentMax / parent.childCount
                    level += step * index
                    parentMax = step
                }
                return level
            }
    }
}