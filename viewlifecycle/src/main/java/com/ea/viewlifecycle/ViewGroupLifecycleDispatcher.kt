package com.ea.viewlifecycle

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

/**
 * A class that is responsible for dispatching lifecycle state to the children of the [viewGroup].
 *
 * Dispatching happens in two cases:
 * - when the lifecycle state of the [viewGroup] is changed.
 * - during a layout pass in the [viewGroup]. Since the layout state of the views may change,
 * dispatcher rebuilds their visibility levels.
 */
internal class ViewGroupLifecycleDispatcher(
        private val viewGroup: ViewGroup) : LifecycleDispatcher(viewGroup) {

    private var lastDispatchRun = 0L

    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        val time = System.currentTimeMillis()
        if (time - lastDispatchRun > dispatchDelay) {
            dispatchLifecycleOnLayout()
            lastDispatchRun = time
        }
    }

    // used to postpone lifecycle state transition in case of layout animations
    private val dispatchDelay: Long

    private val viewComparator = ViewComparator()

    init {
        val wm = viewGroup.context.getSystemService(Context.WINDOW_SERVICE)
                as WindowManager
        val displayRefreshDelay = 1000f / wm.defaultDisplay.refreshRate
        val animationDelay = ValueAnimator.getFrameDelay()

        dispatchDelay = Math.max(displayRefreshDelay.toLong(), animationDelay) * 2

        viewGroup.addOnLayoutChangeListener(layoutListener)
    }

    override fun clear() {
        super.clear()
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