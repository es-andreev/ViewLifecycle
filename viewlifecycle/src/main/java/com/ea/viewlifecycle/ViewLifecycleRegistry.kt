package com.ea.viewlifecycle

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.view.View
import android.view.ViewGroup

internal sealed class ViewLifecycleRegistry(
        lifecycleOwner: LifecycleOwner,
        private val view: View) : LifecycleRegistry(lifecycleOwner) {

    override fun handleLifecycleEvent(event: Event) {
        val state = getStateAfter(event)
        markState(state)
    }

    override fun markState(state: State) {
        try {
            super.markState(state)
        } catch (e: IllegalArgumentException) {
            // This may happen when a view is created, but never actually attached to a window.
            // Its lifecycle then is destroyed while in INITIALIZED state and exception is thrown.
        }

        (view as? ViewGroup)?.viewGroupLifecycleDispatcher?.dispatchLifecycleState(state)

        if (state == State.DESTROYED) {
            (view as? ViewGroup)?.detachViewGroupLifecycleDispatcher()
            view.detachLifecycleOwner()

            if (!view.isBackStackItem) {
                ViewCompanionFragment.get(view)?.apply {
                    activity.supportFragmentManager
                            .beginTransaction()
                            .remove(this)
                            .commitAllowingStateLoss()
                }
            }
            NavViewCompanionFragment.get(view)?.apply {
                activity.supportFragmentManager
                        .beginTransaction()
                        .remove(this)
                        .commitAllowingStateLoss()
            }
        }
    }

    private fun getStateAfter(event: Lifecycle.Event): Lifecycle.State {
        return when (event) {
            Event.ON_CREATE, Event.ON_STOP -> State.CREATED
            Event.ON_START, Event.ON_PAUSE -> State.STARTED
            Event.ON_RESUME -> State.RESUMED
            Event.ON_DESTROY -> State.DESTROYED
            Event.ON_ANY -> {
                throw IllegalArgumentException("Unexpected event value $event.")
            }
        }
    }

    private class ItemViewLifecycleRegistry(lifecycleOwner: LifecycleOwner, view: View) :
            ViewLifecycleRegistry(lifecycleOwner, view)

    private class RootViewLifecycleRegistry(
            lifecycleOwner: LifecycleOwner,
            private val view: View) : ViewLifecycleRegistry(lifecycleOwner, view) {

        private val activityLifecycleObserver = GenericLifecycleObserver { _, event ->
            handleLifecycleEvent(event)
        }

        init {
            view.activity.lifecycle.addObserver(activityLifecycleObserver)

            view.onAttached {
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

    companion object Factory {
        fun create(lifecycleOwner: LifecycleOwner, view: View): ViewLifecycleRegistry {
            return if (view === view.safeRoot) {
                RootViewLifecycleRegistry(lifecycleOwner, view)
            } else {
                ItemViewLifecycleRegistry(lifecycleOwner, view)
            }
        }
    }
}