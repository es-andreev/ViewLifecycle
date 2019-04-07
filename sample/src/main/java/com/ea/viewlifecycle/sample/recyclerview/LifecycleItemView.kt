package com.ea.viewlifecycle.sample.recyclerview

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.ea.viewlifecycle.lifecycleOwner
import com.ea.viewlifecycle.sample.R
import kotlinx.android.synthetic.main.view_lifecycle_item.view.*

class LifecycleItemView : LinearLayout, GenericLifecycleObserver {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    lateinit var adapterPosition: () -> Int

    init {
        orientation = HORIZONTAL
        val offset32 = resources.getDimensionPixelOffset(R.dimen.offset_32)
        setPadding(offset32, offset32, offset32, offset32)
        gravity = Gravity.CENTER_VERTICAL

        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_lifecycle_item, this, true)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        lifecycleOwner.lifecycle.addObserver(this)
        positionLabel.text = resources.getString(R.string.item_x, adapterPosition())
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        val lifecycleState = source.lifecycle.currentState
        state.text = lifecycleState.name
        val indicatorRes = if (lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
            R.drawable.bg_started
        } else {
            R.drawable.bg_stopped
        }
        indicator.setBackgroundResource(indicatorRes)
    }
}