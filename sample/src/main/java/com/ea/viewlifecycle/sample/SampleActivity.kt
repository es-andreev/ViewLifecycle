package com.ea.viewlifecycle.sample

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.FrameLayout
import com.ea.viewlifecycle.trackNavigation
import kotlinx.android.synthetic.main.activity_sample.*

class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        if (savedInstanceState == null) {
            fillView()
        }
        buttonDialog.setOnClickListener {
            startActivity(Intent(this, DialogActivity::class.java))
        }
    }

    private fun fillView(): View {
        val viewsCount = 3
        val display = windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)
        val minSize = Math.min(point.x, point.y) / (viewsCount + 1)
        val maxSize = Math.min(point.x, point.y)

        for (i in 0 until viewsCount) {
            val view = LifecycleStateView(this)
            val size = (minSize + (maxSize - minSize) * i.toFloat() / viewsCount).toInt()
            val lp = FrameLayout.LayoutParams(size, size)
            view.translationY = maxSize - minSize - size.toFloat()

            motionView.addView(view, lp)
        }

        motionView.trackNavigation()
        return motionView
    }
}