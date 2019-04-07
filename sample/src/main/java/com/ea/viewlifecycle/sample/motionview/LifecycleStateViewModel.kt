package com.ea.viewlifecycle.sample.motionview

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.graphics.Point
import android.widget.FrameLayout
import kotlin.math.min

class LifecycleStateViewModel : ViewModel() {

    val liveLayoutParams = MutableLiveData<FrameLayout.LayoutParams>()
    val liveTranslationX = MutableLiveData<Float>()
    val liveTranslationY = MutableLiveData<Float>()

    private var initialised = false

    fun start(index: Int, displaySize: Point, offset16: Int) {
        if (initialised) {
            return
        }

        val viewsCount = SampleMotionView.viewsCount

        val maxSize = min(displaySize.x, displaySize.y) - offset16 * 2
        val minSize = maxSize / (viewsCount + 1)

        val size = (minSize + (maxSize - minSize) * index.toFloat() / viewsCount).toInt()
        val width = size + (viewsCount - index - 1) * minSize / 4
        val height = size * 3 / 4

        liveLayoutParams.value = FrameLayout.LayoutParams(width, height)
        liveTranslationX.value = (viewsCount - index - 1) * offset16.toFloat()
        liveTranslationY.value = (viewsCount - index - 1) * 3 * offset16.toFloat()

        initialised = true
    }
}