package com.viewlifecycle

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

internal sealed class ViewLifecycleRegistry(
        lifecycleOwner: LifecycleOwner,
        private val view: View) : LifecycleRegistry(lifecycleOwner) {

    override fun handleLifecycleEvent(event: Event) {
        val state = getStateAfter(event)
        currentState = state
    }

    override fun setCurrentState(state: State) {
        try {
            super.setCurrentState(state)
        } catch (e: IllegalArgumentException) {
            // This may happen when a view is created, but never actually attached to a window.
            // Its lifecycle then is destroyed while in INITIALIZED state and exception is thrown.
        }

        (view as? ViewGroup)?.viewGroupLifecycleDispatcher?.dispatchLifecycleState(state)

        if (state == State.DESTROYED) {
            (view as? ViewGroup)?.detachViewGroupLifecycleDispatcher()
            view.detachLifecycleOwner()

            // activity is finishing, or view was removed from parent
            if (view.safeActivity?.isFinishing == true || !ViewCompat.isAttachedToWindow(view)) {
                if (!view.isBackStackItem) {
                    ViewCompanionFragment.get(view)?.apply {
                        requireActivity().supportFragmentManager
                                .beginTransaction()
                                .remove(this)
                                .commitAllowingStateLoss()
                    }
                }
                NavViewCompanionFragment.get(view)?.apply {
                    requireActivity().supportFragmentManager
                            .beginTransaction()
                            .remove(this)
                            .commitAllowingStateLoss()
                }
            }
        }
    }

    private fun getStateAfter(event: Event): State {
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

        private val activityLifecycleObserver = LifecycleEventObserver { _, event ->
            handleLifecycleEvent(event)
        }

        init {
            view.activity.lifecycle.addObserver(activityLifecycleObserver)

            view.onAttached {
                currentState = view.activity.lifecycle.currentState
            }
        }

        override fun setCurrentState(state: State) {
            super.setCurrentState(state)

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