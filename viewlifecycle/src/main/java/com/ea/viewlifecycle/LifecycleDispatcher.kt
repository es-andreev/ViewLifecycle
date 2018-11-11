package com.ea.viewlifecycle

import android.arch.lifecycle.Lifecycle
import android.graphics.Region
import android.support.annotation.CallSuper
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.view.View

/**
 * Base class for dispatching lifecycle state.
 * In order to dispatch, it builds a list of views sorted according to their visibility level.
 * Currently visible views, i.e. having at least one pixel that is not overlapped
 * by other views, belong to level 0. Views at levels greater than 0 cannot have
 * lifecycle state greater than [Lifecycle.State.CREATED].
 */
internal abstract class LifecycleDispatcher(private val view: View) {

    private var lastLayoutLevels = arrayListOf<ViewLevelData>()

    private var lastDispatchedState: Lifecycle.State? = null

    private val xy = IntArray(2)

    abstract fun getZSortedViews(): Array<View>

    @CallSuper
    internal open fun attach() {
    }

    @CallSuper
    internal open fun detach() {
        lastLayoutLevels.clear()
    }

    internal fun dispatchLifecycleState(state: Lifecycle.State) {
        if (state == lastDispatchedState) {
            return
        }
        var stateToDispatch = state
        if (!view.isDisplayed && state.isAtLeast(Lifecycle.State.CREATED)) {
            stateToDispatch = Lifecycle.State.CREATED
        }
        if (lastLayoutLevels.isEmpty()) {
            lastLayoutLevels = buildLayoutLevels(getZSortedViews())
            if (lastLayoutLevels.isEmpty()) {
                lastDispatchedState = stateToDispatch
                return
            }
        }

        for (i in 0 until lastLayoutLevels.size) {
            lastLayoutLevels[i].updateState(stateToDispatch)
        }
        lastDispatchedState = stateToDispatch
    }

    internal fun dispatchLifecycleOnLayout() {
        val owner = view.rawLifecycleOwner
                ?: throw IllegalStateException("LifecycleDispatcher is attached " +
                        "but View's LifecycleOwner is null.")

        val currentState = owner.lifecycle.currentState

        val newLevels = buildLayoutLevels(getZSortedViews())

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = lastLayoutLevels.size
            override fun getNewListSize() = newLevels.size

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return lastLayoutLevels[oldPos].visibility == newLevels[newPos].visibility &&
                        lastLayoutLevels[oldPos].level == newLevels[newPos].level
            }

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return lastLayoutLevels[oldPos].view === newLevels[newPos].view
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback, true)
        diffResult.dispatchUpdatesTo(object : ListUpdateCallback {
            override fun onChanged(position: Int, count: Int, payload: Any?) {
                for (index in position until position + count) {
                    newLevels
                            .firstOrNull { it.view == lastLayoutLevels[index].view }
                            ?.updateState(currentState)
                }
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                lastLayoutLevels[fromPosition].updateState(currentState)
                newLevels[toPosition].updateState(currentState)
            }

            override fun onInserted(position: Int, count: Int) {
                for (index in position until position + count) {
                    newLevels[index].updateState(currentState)
                }
            }

            override fun onRemoved(position: Int, count: Int) {
                for (index in position until position + count) {
                    lastLayoutLevels[index].view.destroy()
                }
            }
        })

        lastLayoutLevels = newLevels
        lastDispatchedState = currentState
    }

    private fun buildLayoutLevels(zSortedViews: Array<View>): ArrayList<ViewLevelData> {
        val levelViews = ArrayList<ViewLevelData>()
        val levels = ArrayList<Region>()

        zSortedViews.forEach {
            it.getLocationInWindow(xy)
            val viewRegion = Region(xy[0], xy[1], xy[0] + it.width, xy[1] + it.height)

            // find view level
            var viewLevel = 0
            for (i in levels.size - 1 downTo 0) {
                val region = levels[i]
                val unionRegion = Region(region)
                unionRegion.op(viewRegion, Region.Op.UNION)
                if (unionRegion == region) {
                    // view is completely hidden behind the i'th region
                    viewLevel = i + 1
                }
            }

            // the level is discovered, save it
            if (viewLevel >= levels.size) {
                // view is behind the last level, insert new
                levels += viewRegion
                levelViews += ViewLevelData.of(it, levels.size - 1)
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
                levelViews.add(levelIndex, ViewLevelData.of(it, viewLevel))
            }
        }

        return levelViews
    }
}