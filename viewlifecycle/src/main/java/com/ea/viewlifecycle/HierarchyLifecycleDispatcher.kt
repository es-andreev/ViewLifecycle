package com.ea.viewlifecycle

import android.view.View
import android.view.ViewGroup
import java.util.*

/**
 * A lifecycle dispatcher that handles multiple [ViewGroupLifecycleDispatcher]s.
 */
internal class HierarchyLifecycleDispatcher(rootView: ViewGroup) : LifecycleDispatcher(rootView) {

    private var viewGroups = hashSetOf<ViewGroup>()

    private val viewGroupComparator = ViewGroupComparator()

    private val stemChangeListener = object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewRemoved(parent: View?, child: View?) {
            child?.destroy()

            if (child is ViewGroup && child.subtreeDispatchers > 0) {
                parent?.innerStem?.forEach {
                    it.decrementStemListener(child.subtreeDispatchers)
                }
            }

            dispatchLifecycleOnLayout()
        }

        override fun onChildViewAdded(parent: View?, child: View?) {
            dispatchLifecycleOnLayout()
        }
    }

    init {
        rootView.level = 0
    }

    internal fun addViewGroup(viewGroup: ViewGroup) {
        if (viewGroups.add(viewGroup)) {
            viewGroup.innerStem.forEach {
                it.incrementStemListener(1)
            }

            dispatchLifecycleOnLayout()
        }
    }

    internal fun removeViewGroup(viewGroup: ViewGroup) {
        if (viewGroups.remove(viewGroup)) {
            viewGroup.innerStem.forEach {
                it.decrementStemListener(viewGroup.subtreeDispatchers)
            }

            dispatchLifecycleOnLayout()
        }
    }

    override fun clear() {
        super.clear()
        viewGroups.forEach { it.setOnHierarchyChangeListener(null) }
        viewGroups.clear()
    }

    override fun getZSortedViews(): Collection<View> {
        return viewGroups.sortedWith(viewGroupComparator)
    }

    private fun ViewGroup.decrementStemListener(value: Int) {
        subtreeDispatchers -= value

        if (subtreeDispatchers == 0) {
            setOnHierarchyChangeListener(null)
        }
    }

    private fun ViewGroup.incrementStemListener(value: Int) {
        subtreeDispatchers += value

        setOnHierarchyChangeListener(stemChangeListener)
    }

    private var ViewGroup.subtreeDispatchers: Int
        get() = getTag(R.id.subtree_dispatchers) as? Int ?: 0
        set(value) = setTag(R.id.subtree_dispatchers, maxOf(0, value))

    override fun buildLayoutLevels(): ArrayList<View> {
        return super.buildLayoutLevels().apply {
            forEach { view ->
                if (view.level == 0) {
                    view.innerStem.forEach {
                        it.level = 0
                    }
                }
            }
        }
    }

    private class ViewGroupComparator : Comparator<ViewGroup> {
        override fun compare(v1: ViewGroup, v2: ViewGroup): Int {
            if (v1 === v2) {
                return 0
            }

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

        @Suppress("LocalVariableName")
        private val ViewGroup.hierarchyLevel: Int
            get() {
                var parentMax = Int.MAX_VALUE
                var level = 0
                val _fullStem = fullStem

                for (i in _fullStem.size - 1 downTo 1) {
                    val parent = _fullStem[i]
                    val child = _fullStem[i - 1]
                    val index = parent.indexOfChild(child) + 1
                    if (index == 0) {
                        throw IllegalStateException("Wrong hierarchy state: " +
                                "$parent is not a parent of $child.")
                    }

                    val step = parentMax / parent.childCount
                    level += step * index
                    parentMax = step
                }
                return level
            }
    }
}