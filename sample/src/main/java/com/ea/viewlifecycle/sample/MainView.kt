package com.ea.viewlifecycle.sample

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.ea.viewlifecycle.lifecycleOwner
import com.ea.viewlifecycle.sample.motionview.SampleMotionView
import com.ea.viewlifecycle.sample.recyclerview.SampleRecyclerView
import kotlinx.android.synthetic.main.view_main.view.*

class MainView : LinearLayout, LifecycleObserver {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        lifecycleOwner.lifecycle.addObserver(this)

        LayoutInflater.from(context).inflate(R.layout.view_main, this, true)

        sampleMotion.setOnClickListener {
            navigator.navigateForward(SampleMotionView(context))
        }

        sampleRecycler.setOnClickListener {
            navigator.navigateForward(SampleRecyclerView(context))
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        activity.title = resources.getString(R.string.app_name)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}