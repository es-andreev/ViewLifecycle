package com.ea.viewlifecycle

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.view.View

internal class ViewRootLifecycleRegistry(
        lifecycleOwner: LifecycleOwner, view: View) : ViewLifecycleRegistry(lifecycleOwner, view) {

    private val activityLifecycleObserver = GenericLifecycleObserver { _, event ->
        handleLifecycleEvent(event)
    }

    init {
        view.activity.lifecycle.addObserver(activityLifecycleObserver)
    }

    override fun markState(state: Lifecycle.State) {
        viewRef.get()?.hierarchyLifecycleDispatcher?.dispatchLifecycleState(state)

        super.markState(state)

        if (state == State.DESTROYED) {
            viewRef.get()?.activity?.lifecycle?.removeObserver(activityLifecycleObserver)
        }
    }
}