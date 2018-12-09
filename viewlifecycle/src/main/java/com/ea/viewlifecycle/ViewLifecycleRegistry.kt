package com.ea.viewlifecycle

import android.arch.lifecycle.*
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference

// TODO refactor to root registry
internal class ViewLifecycleRegistry(lifecycleOwner: LifecycleOwner, v: View) : LifecycleRegistry(lifecycleOwner) {

    private val viewRef = WeakReference(v)

    init {
        val activityLifecycleObserver = GenericLifecycleObserver { _, event ->
            //            viewRef.get()?.post { handleLifecycleEvent(event) }
            handleLifecycleEvent(event)
        }

        val lifecycleObserver = object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                // TODO move to doMarkState()
                val view = viewRef.get()

                (view as? ViewGroup)?.detachLifecycleDispatcher()
                view?.rawLifecycleOwner = null
                view?.lifecycleOwner = LazyLifecycleOwnerDelegate.NullLifecycleOwner

                view?.activity?.lifecycle?.removeObserver(activityLifecycleObserver)
                removeObserver(this)
            }
        }

        v.activity.lifecycle.addObserver(activityLifecycleObserver)
        addObserver(lifecycleObserver)
    }

    override fun handleLifecycleEvent(event: Event) {
        val state = getStateAfter(event)
        markState(state)
    }

    override fun markState(state: State) {
        if (viewRef.get()?.needMarkState() == true) {
            doMarkState(state)
        }
    }

    internal fun forceMarkState(state: State) {
        doMarkState(state)
    }

    private fun doMarkState(state: State) {
        viewRef.get()?.hierarchyLifecycleDispatcher?.dispatchLifecycleState(state)
        viewRef.get()?.viewGroupLifecycleDispatcher?.dispatchLifecycleState(state)

        when (state) {
            State.DESTROYED,
            State.INITIALIZED,
            State.CREATED -> super.markState(state)

            State.STARTED,
            State.RESUMED -> {
                if (viewRef.get()?.isDisplayed == true) {
                    super.markState(state)
                }
            }
        }
    }

    private fun View.needMarkState(): Boolean {
        return hierarchyLifecycleDispatcher != null
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
}