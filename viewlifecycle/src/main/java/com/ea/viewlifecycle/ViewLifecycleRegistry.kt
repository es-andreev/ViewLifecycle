package com.ea.viewlifecycle

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference

internal open class ViewLifecycleRegistry(
        lifecycleOwner: LifecycleOwner, view: View) : LifecycleRegistry(lifecycleOwner) {

    protected val viewRef = WeakReference(view)

    override fun handleLifecycleEvent(event: Event) {
        val state = getStateAfter(event)
        markState(state)
    }

    override fun markState(state: State) {
        val view = viewRef.get()

        view?.viewGroupLifecycleDispatcher?.dispatchLifecycleState(state)

        when (state) {
            State.DESTROYED -> {
                (view as? ViewGroup)?.detachViewGroupLifecycleDispatcher()
                view?.detachLifecycleOwner()

                super.markState(state)
            }
            State.INITIALIZED,
            State.CREATED -> super.markState(state)

            State.STARTED,
            State.RESUMED -> {
                if (view?.isDisplayed == true) {
                    super.markState(state)
                }
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

    companion object {
        fun create(lifecycleOwner: LifecycleOwner, view: View): ViewLifecycleRegistry {
            return if (view === view.safeRoot) {
                ViewRootLifecycleRegistry(lifecycleOwner, view)
            } else {
                ViewLifecycleRegistry(lifecycleOwner, view)
            }
        }
    }
}