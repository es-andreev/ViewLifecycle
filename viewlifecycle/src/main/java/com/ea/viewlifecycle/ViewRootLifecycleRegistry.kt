package com.ea.viewlifecycle

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.view.View

internal class ViewRootLifecycleRegistry(
        lifecycleOwner: LifecycleOwner,
        private val view: View) : ViewLifecycleRegistry(lifecycleOwner, view) {

    private val activityLifecycleObserver = GenericLifecycleObserver { _, event ->
        handleLifecycleEvent(event)
    }

    init {
        view.activity.lifecycle.addObserver(activityLifecycleObserver)

        view.afterMeasured {
            markState(view.activity.lifecycle.currentState)
        }
    }

    override fun markState(state: Lifecycle.State) {
        super.markState(state)

        view.hierarchyLifecycleDispatcher?.dispatchLifecycleState(state)

        if (state == State.DESTROYED) {
            view.safeActivity?.lifecycle?.removeObserver(activityLifecycleObserver)
        }
    }
}