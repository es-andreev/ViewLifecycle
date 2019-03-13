package com.ea.viewlifecycle

import android.arch.lifecycle.Lifecycle
import android.graphics.Region
import android.support.annotation.CallSuper
import android.view.View

/**
 * Base class for dispatching lifecycle state.
 * In order to dispatch, it builds a list of views sorted according to their visibility level.
 * Currently visible views, i.e. having at least one pixel that is not overlapped
 * by other views, belong to level 0. Views at levels greater than 0 cannot have
 * lifecycle state greater than [Lifecycle.State.CREATED].
 */
internal abstract class LifecycleDispatcher(private val view: View) {

    private var lastDispatchedState: Lifecycle.State? = null

    private val xy = IntArray(2)

    abstract fun getZSortedViews(): Collection<View>

    @CallSuper
    internal open fun clear() {
    }

    internal fun dispatchLifecycleState(state: Lifecycle.State) {
        if (state == lastDispatchedState) {
            return
        }
        var stateToDispatch = state
        if (!view.isDisplayed && state.isAtLeast(Lifecycle.State.CREATED)) {
            stateToDispatch = Lifecycle.State.CREATED
        }
        if (stateToDispatch == lastDispatchedState) {
            return
        }

        val newLevels = buildLayoutLevels()
        if (newLevels.isEmpty()) {
            lastDispatchedState = stateToDispatch
            return
        }

        newLevels.forEach {
            it.updateState(stateToDispatch)
        }
        lastDispatchedState = stateToDispatch
    }

    internal open fun dispatchLifecycleOnLayout() {
        val owner = view.rawLifecycleOwner ?: return

        val currentState = owner.lifecycle.currentState

        val newLevels = buildLayoutLevels()

        newLevels.forEach {
            it.updateState(currentState)
        }

        lastDispatchedState = currentState
    }

    // TODO must take into account that zSortedViews may contain parents and children -
    // level 0 views must have level 0 stem
    protected open fun buildLayoutLevels(): ArrayList<View> {
        val zSortedViews = getZSortedViews()

        val levelViews = ArrayList<View>()
        val levels = ArrayList<Region>()

        val parentLevel = view.level

        zSortedViews.forEach {
            it.getLocationInWindow(xy)
            val viewRegion = Region(xy[0], xy[1],
                    xy[0] + it.width, xy[1] + it.height)

            // find view level
            var viewLevel = 0
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

            // the level is discovered, save it
            if (viewLevel >= levels.size) {
                // view is behind the last level, insert new
                levels += viewRegion
                it.level = levels.size - 1 + parentLevel
                levelViews += it
            } else {
                // union view's region with that of its level
                levels[viewLevel].op(viewRegion, Region.Op.UNION)

                var levelIndex = 0
                for (i in 0 until levelViews.size) {
                    if (levelViews[i].level == viewLevel) {
                        levelIndex = i
                        break
                    }
                }
                it.level = viewLevel + parentLevel
                levelViews.add(levelIndex, it)
            }
        }

        return levelViews
    }
}