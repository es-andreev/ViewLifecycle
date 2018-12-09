package com.ea.viewlifecycle

import android.arch.lifecycle.Lifecycle
import android.view.View

internal data class ViewLevelData(val view: View, val level: Int, val visibility: Int) {

    fun updateState(state: Lifecycle.State) {
        if (!state.isAtLeast(Lifecycle.State.STARTED) || level == 0 && view.isDisplayed) {
            view.rawLifecycleOwner?.lifecycle?.forceMarkState(state)
        } else {
            view.rawLifecycleOwner?.lifecycle?.forceMarkState(Lifecycle.State.CREATED)
        }
    }

    companion object Factory {
        fun of(view: View, level: Int) = ViewLevelData(view, level, view.visibility)
    }
}