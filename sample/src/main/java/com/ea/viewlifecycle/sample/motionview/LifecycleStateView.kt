package com.ea.viewlifecycle.sample.motionview

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.ea.viewlifecycle.lifecycleOwner
import com.ea.viewlifecycle.sample.R
import com.ea.viewlifecycle.sample.activity
import com.ea.viewlifecycle.viewModelProvider

class LifecycleStateView : TextView, GenericLifecycleObserver {

    private lateinit var viewModel: LifecycleStateViewModel

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var index: Int = 0

    init {
        val offset8 = resources.getDimensionPixelOffset(R.dimen.offset_8)
        setPadding(offset8, offset8, offset8, offset8)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_12))

        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                onCreate()
            }

            Lifecycle.Event.ON_DESTROY -> {
                onDestroy()
            }
        }

        updateOnStateChanged(source, event)
    }

    private fun onCreate() {
        visibility = View.GONE

        val offset16 = resources.getDimensionPixelOffset(R.dimen.offset_16)
        val display = activity.windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)

        viewModel = viewModelProvider.get(LifecycleStateViewModel::class.java)
        viewModel.start(index, point, offset16)
        viewModel.liveLayoutParams.observe(lifecycleOwner, Observer {
            it?.apply {
                layoutParams = this
            }
        })
        viewModel.liveTranslationX.observe(lifecycleOwner, Observer {
            it?.apply { translationX = this }
        })
        viewModel.liveTranslationY.observe(lifecycleOwner, Observer {
            it?.apply { translationY = this }
        })

        postDelayed({
            visibility = View.VISIBLE
        }, index * 1000L)
    }

    private fun onDestroy() {
        if (visibility == View.VISIBLE) {
            // save state in ViewModel
            viewModel.liveLayoutParams.value = layoutParams as FrameLayout.LayoutParams?
            viewModel.liveTranslationX.value = translationX
            viewModel.liveTranslationY.value = translationY
        }
    }

    private fun updateOnStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        text = event.name

        val state = source.lifecycle.currentState
        if (state.isAtLeast(Lifecycle.State.RESUMED)) {
            setBackgroundResource(R.drawable.bg_started_44)
        } else {
            setBackgroundResource(R.drawable.bg_stopped_44)
        }
    }
}