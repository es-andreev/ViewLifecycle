package com.viewlifecycle.sample.motionview

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.viewlifecycle.lifecycleOwner
import com.viewlifecycle.sample.R
import com.viewlifecycle.sample.activity
import kotlinx.android.synthetic.main.view_sample_motion.view.*

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
        for (i in 0 until viewsCount) {
            val view = LifecycleStateView(activity).apply {
                id = viewIds[i]
                index = i
            }
            motionView.addView(view)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        activity.title = resources.getString(R.string.sample_motion)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    companion object {
        const val viewsCount = 3
    }
}