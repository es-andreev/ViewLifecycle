package com.ea.viewlifecycle.sample.motionview

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.ea.viewlifecycle.lifecycleOwner
import com.ea.viewlifecycle.sample.R
import com.ea.viewlifecycle.sample.activity
import kotlinx.android.synthetic.main.view_sample_motion.view.*
import kotlin.math.min

class SampleMotionView : FrameLayout, LifecycleObserver {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val viewIds = arrayOf(
            R.id.view_lifecycle_state_1,
            R.id.view_lifecycle_state_2,
            R.id.view_lifecycle_state_3
    )

    init {
        lifecycleOwner.lifecycle.addObserver(this)

        setBackgroundColor(Color.WHITE)
        clipChildren = false
        LayoutInflater.from(context).inflate(R.layout.view_sample_motion, this, true)

        fillView()
        buttonDialog.setOnClickListener {
            activity.startActivity(Intent(activity, DialogActivity::class.java))
        }
    }

    private fun fillView() {
        val viewsCount = 3

        val display = activity.windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)

        val offset16 = resources.getDimensionPixelOffset(R.dimen.offset_16)
        val maxSize = min(point.x, point.y) - offset16 * 2
        val minSize = maxSize / (viewsCount + 1)

        for (i in 0 until viewsCount) {
            val view = LifecycleStateView(activity)
            view.id = viewIds[i]
            val size = (minSize + (maxSize - minSize) * i.toFloat() / viewsCount).toInt()
            val width = size + (viewsCount - i - 1) * minSize / 4
            val height = size * 3 / 4
            val lp = FrameLayout.LayoutParams(width, height)
            view.translationY = (viewsCount - i - 1) * 3 * offset16.toFloat()
            view.translationX = (viewsCount - i - 1) * offset16.toFloat()

            motionView.addView(view, lp)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        activity.title = resources.getString(R.string.sample_motion)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}