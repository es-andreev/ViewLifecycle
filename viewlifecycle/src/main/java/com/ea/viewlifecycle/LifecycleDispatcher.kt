package com.ea.viewlifecycle

import android.arch.lifecycle.Lifecycle
import android.graphics.Region
import android.view.View

/**
 * Base class for dispatching lifecycle state.
 * In order to dispatch, it builds a list of views sorted according to their visibility level.
 * Currently visible views, i.e. having at least one pixel that is not overlapped
 * by other views, belong to level 0. Views at levels greater than 0 cannot have
 * lifecycle state greater than [Lifecycle.State.CREATED].
 */
internal abstract class LifecycleDispatcher(private val view: View) {

    private val xy = IntArray(2)

    internal abstract fun getZSortedViews(): Collection<View>

    internal fun dispatchLifecycleState(state: Lifecycle.State) {
        val newLevels = buildLayoutLevels()
        if (newLevels.isEmpty()) {
            return
        }

        newLevels.forEach {
            it.updateState(state)
        }
    }

    internal open fun dispatchLifecycleOnLayout() {
        val owner = view.rawLifecycleOwner ?: return
        val currentState = owner.lifecycle.currentState

        dispatchLifecycleState(currentState)
    }

    protected open fun buildLayoutLevels(): ArrayList<View> {
        val zSortedViews = getZSortedViews()

        val levelViews = ArrayList<View>()
        val levels = ArrayList<Region>()

        val parentLevel = view.level

        zSortedViews.forEach {

            val viewLevel = it.findLevel(levels)

            if (viewLevel.level < levels.size) {
                it.visibleRegion = Region(viewLevel.region).apply {
                    op(levels[viewLevel.level], Region.Op.DIFFERENCE)
                }
            }

            it.level = viewLevel.level + parentLevel

            // the level is discovered, save it
            if (viewLevel.level >= levels.size) {
                // view is behind the last level, insert new
                levels += viewLevel.region
                levelViews += it
            } else {
                // union view's region with that of its level
                levels[viewLevel.level].op(viewLevel.region, Region.Op.UNION)

                var levelIndex = 0
                for (i in 0 until levelViews.size) {
                    if (levelViews[i].level == viewLevel.level) {
                        levelIndex = i
                        break
                    }
                }
                levelViews.add(levelIndex, it)
            }
        }

        return levelViews
    }

    private fun View.findLevel(levels: ArrayList<Region>): ViewLevel {
        getLocationInWindow(xy)
        val viewRegion = Region(xy[0], xy[1],
                xy[0] + measuredWidth, xy[1] + measuredHeight)

        var viewLevel = 0

        view.visibleRegion?.apply {
            viewRegion.op(this, Region.Op.INTERSECT)
            if (viewRegion.isEmpty) {
                viewLevel = 1
            }
        }

        for (i in levels.size - 1 downTo 0) {
            val region = levels[i]
            val unionRegion = Region(region)
            unionRegion.op(viewRegion, Region.Op.UNION)
            if (unionRegion == region) {
                // view is completely hidden behind the i'th region
                viewLevel = i + 1
                break
            }
        }
        return ViewLevel(viewLevel, viewRegion)
    }

    private data class ViewLevel(val level: Int, val region: Region)
}