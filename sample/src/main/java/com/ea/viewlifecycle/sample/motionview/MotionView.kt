package com.ea.viewlifecycle.sample.motionview

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

/**
 * A [FrameLayout] whose children can be moved.
 */
class MotionView : FrameLayout {

    private var selectedView: View? = null
    private val lastPoint = PointF()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> selectedView = findViewUnder(event.x, event.y)
            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_UP -> selectedView = null
        }
        lastPoint.x = event.x
        lastPoint.y = event.y
        return true
    }

    private fun onMove(event: MotionEvent) {
        val dx = event.x - lastPoint.x
        val dy = event.y - lastPoint.y

        val selected = selectedView
        if (selected != null) {
            selected.translationX = selected.translationX + dx
            selected.translationY = selected.translationY + dy
            requestLayout()
        }
    }

    private fun findViewUnder(x: Float, y: Float): View? {
        var v: View?
        for (i in childCount - 1 downTo 0) {
            v = getChildAt(i)
            if (isPointInBounds(x, y, v.left.toFloat(), v.top.toFloat(),
                            v.right.toFloat(), v.bottom.toFloat(), v)) {
                return v
            }
        }
        return null
    }

    private fun isPointInBounds(pointX: Float, pointY: Float,
                                l: Float, t: Float, r: Float, b: Float,
                                view: View?): Boolean {

        val point0 = floatArrayOf(l, t)
        val point1 = floatArrayOf(r, t)
        val point2 = floatArrayOf(r, b)
        val point3 = floatArrayOf(l, b)

        if (view != null) {
            val w = view.width
            val h = view.height
            val centerX = view.left + w / 2
            val centerY = view.top + h / 2

            val matrix = Matrix()
            matrix.setScale(view.scaleX, view.scaleY, centerX.toFloat(), centerY.toFloat())
            matrix.postRotate(view.rotation, centerX.toFloat(), centerY.toFloat())
            matrix.postTranslate(view.translationX, view.translationY)

            matrix.mapPoints(point0)
            matrix.mapPoints(point1)
            matrix.mapPoints(point2)
            matrix.mapPoints(point3)
        }

        val points = arrayOf(
                PointF(point0[0], point0[1]),
                PointF(point1[0], point1[1]),
                PointF(point2[0], point2[1]),
                PointF(point3[0], point3[1]),
                PointF(point0[0], point0[1]))

        return isPointInPolygon(PointF(pointX, pointY), points)
    }

    private fun isPointInPolygon(tap: PointF, vertices: Array<PointF>): Boolean {
        var intersectCount = 0
        for (j in 0 until vertices.size - 1) {
            if (rayCastIntersect(tap, vertices[j], vertices[j + 1])) {
                intersectCount++
            }
        }

        return intersectCount % 2 == 1
    }

    private fun rayCastIntersect(tap: PointF, vertA: PointF, vertB: PointF): Boolean {
        val aY = vertA.y
        val bY = vertB.y
        val aX = vertA.x
        val bX = vertB.x
        val pY = tap.y
        val pX = tap.x

        if (aY > pY && bY > pY || aY < pY && bY < pY || aX < pX && bX < pX) {
            return false
        }

        if (Math.abs(aX - bX) < 0.0001) {
            return aX > pX
        }

        val m = (aY - bY) / (aX - bX)
        val bee = -aX * m + aY
        val x = (pY - bee) / m

        return x > pX
    }
}
