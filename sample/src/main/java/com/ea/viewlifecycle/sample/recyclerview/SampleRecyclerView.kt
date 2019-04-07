package com.ea.viewlifecycle.sample.recyclerview

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.graphics.Color
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.ea.viewlifecycle.lifecycleOwner
import com.ea.viewlifecycle.sample.R
import com.ea.viewlifecycle.sample.activity
import kotlinx.android.synthetic.main.view_sample_recycler.view.*

class SampleRecyclerView : FrameLayout, LifecycleObserver {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        lifecycleOwner.lifecycle.addObserver(this)

        setBackgroundColor(Color.WHITE)
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_sample_recycler, this, true)

        recycler.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
            adapter = LifecycleAdapter()
            lifecycleOwner
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        recyclerView.requestLayout()
                    }
                }
            })
        }

        overlayView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (overlayView.measuredWidth > 0) {
                    overlayView.viewTreeObserver.removeOnPreDrawListener(this)
                    overlayView.layoutParams.height = height / 2
                    overlayView.requestLayout()
                    return false
                }
                return true
            }
        })
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onStart() {
        activity.title = resources.getString(R.string.sample_recycler)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class LifecycleViewHolder(view: LifecycleItemView) : RecyclerView.ViewHolder(view) {

        companion object {
            fun create(parent: ViewGroup): LifecycleViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(
                        R.layout.view_lifecycle_holder, parent, false)
                view as LifecycleItemView

                val viewHolder = LifecycleViewHolder(view)
                view.adapterPosition = { viewHolder.adapterPosition }
                return viewHolder
            }
        }
    }

    class LifecycleAdapter : RecyclerView.Adapter<LifecycleViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, position: Int) =
                LifecycleViewHolder.create(parent)

        override fun getItemCount() = 20

        override fun onBindViewHolder(viewHolder: LifecycleViewHolder, position: Int) {
        }
    }
}