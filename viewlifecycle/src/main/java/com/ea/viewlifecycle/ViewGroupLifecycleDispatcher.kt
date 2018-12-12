package com.ea.viewlifecycle

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
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
internal class ViewGroupLifecycleDispatcher(private val viewGroup: ViewGroup) : LifecycleDispatcher(viewGroup) {

    private val handler = Handler(Looper.getMainLooper())

    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        handler.removeCallbacks(dispatchOnLayoutRun)
        handler.postDelayed(dispatchOnLayoutRun, dispatchDelay)
    }

    private val dispatchOnLayoutRun = {
        dispatchLifecycleOnLayout()
    }

    // used to postpone lifecycle state transition in case of layout animations
    private val dispatchDelay: Long

    private val viewLevelComparator: Comparator<View> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewLevelComparator()
        } else {
            ViewLevelComparatorSupport()
        }
    }

    init {
        val wm = viewGroup.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayRefreshDelay = 1000f / wm.defaultDisplay.refreshRate
        val animationDelay = ValueAnimator.getFrameDelay()
        dispatchDelay = Math.max(displayRefreshDelay.toLong(), animationDelay) * 2

        viewGroup.addOnLayoutChangeListener(layoutListener)
    }

    override fun clear() {
        super.clear()
        viewGroup.removeOnLayoutChangeListener(layoutListener)
        handler.removeCallbacks(dispatchOnLayoutRun)
    }

    override fun getZSortedViews(): Collection<View> {
        // sort views by z order: top displayed (elevation + z translation) come first
        val zSortedViews = ArrayList<View>()
        for (i in viewGroup.childCount - 1 downTo 0) {
            zSortedViews.add(viewGroup.getChildAt(i))
        }
        return zSortedViews.apply {
            sortWith(viewLevelComparator)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class ViewLevelComparator : Comparator<View> {
        override fun compare(v1: View, v2: View): Int {
            return (v2.z - v1.z).toInt()
        }
    }

    private class ViewLevelComparatorSupport : Comparator<View> {
        override fun compare(v1: View, v2: View) = 0
    }
}