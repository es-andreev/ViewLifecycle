package com.ea.viewlifecycle

import android.os.Build
import android.view.View
import android.view.ViewGroup

/**
 * A class that is responsible for dispatching lifecycle state to the children of the [viewGroup].
 */
internal class ViewGroupLifecycleDispatcher(
        private val viewGroup: ViewGroup) : LifecycleDispatcher(viewGroup) {

    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        dispatchLifecycleOnLayout()
    }

    private val viewComparator = ViewComparator()

    init {
        viewGroup.addOnLayoutChangeListener(layoutListener)
    }

    internal fun clear() {
        viewGroup.removeOnLayoutChangeListener(layoutListener)
    }

    override fun getZSortedViews(): Collection<View> {
        val zSortedViews = ArrayList<View>()
        for (i in viewGroup.childCount - 1 downTo 0) {
            zSortedViews.add(viewGroup.getChildAt(i))
        }
        return zSortedViews.sortedWith(viewComparator)
    }

    private class ViewComparator : Comparator<View> {
        override fun compare(v1: View, v2: View): Int {
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
                val v1HasContent = v1 !is ViewGroup || v1.childCount > 0
                val v2HasContent = v2 !is ViewGroup || v2.childCount > 0

                return if (!v1HasContent && !v2HasContent) {
                    0
                } else if (v2HasContent && !v1HasContent) {
                    1
                } else if (v1HasContent && !v2HasContent) {
                    -1
                } else {
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        (v2.z - v1.z).toInt()
                    } else {
                        0
                    }
                }
            }
        }
    }
}