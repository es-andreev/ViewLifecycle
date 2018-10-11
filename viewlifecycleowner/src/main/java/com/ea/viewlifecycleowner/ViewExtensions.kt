package com.ea.viewlifecycleowner

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ContextWrapper
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup

/**
 * Obtain a [LifecycleOwner] of the view. View's lifecycle depends on:
 * - the lifecycle of the owning activity
 * - whether it's attached to a window (items in a dynamic ViewGroups like RecyclerView
 * can have a proper lifecycle)
 * - the layout position in the parent ViewGroup if it is a navigation container,
 * i.e. [attachNavigation] was called on it. See [ViewLifecycleDispatcher].
 */
var View.lifecycleOwner: LifecycleOwner by LazyLifecycleOwnerDelegate {
    ViewLifecycleOwner(this).also {
        rawLifecycleOwner = it
    }
}
    internal set

internal var View.rawLifecycleOwner: ViewLifecycleOwner? by HolderDelegate()

val View.viewModelProvider: ViewModelProvider
    get() {
        if (!checkLifecycleState(Lifecycle.State.CREATED)) {
            throw IllegalStateException("Cannot create ViewModelProvider until " +
                    "LifecycleOwner is in created state.")
        }
        return ViewModelProviders.of(getOrCreateCompanionFragment())
    }

var View.arguments: Bundle?
    get() = companionFragment?.arguments
    set(value) {
        if (!checkLifecycleState(Lifecycle.State.CREATED)) {
            throw IllegalStateException("Cannot set arguments until " +
                    "LifecycleOwner is in created state.")
        }
        getOrCreateCompanionFragment().arguments = value
    }

fun ViewGroup.attachNavigation() {
    detachNavigation()

    post {
        lifecycleOwner // ensure ViewLifecycleOwner is initialised
        getOrCreateCompanionFragment() // ensure ViewCompanionFragment is attached

        ViewLifecycleDispatcher(this).apply {
            attach()
            rawLifecycleOwner?.lifecycle?.dispatcher = this
            requestLayout()
        }
    }
}

fun ViewGroup.detachNavigation() {
    rawLifecycleOwner?.lifecycle?.dispatcher?.detach()
    rawLifecycleOwner?.lifecycle?.dispatcher = null
}

fun View.destroy() {
    if (tag === ViewDestroyed) {
        return
    }

    if (this is ViewGroup) {
        detachNavigation()

        for (i in 0 until childCount) {
            getChildAt(i).destroy()
        }
        if (this is RecyclerView) {
            adapter = null
            val destroyablePool = recycledViewPool as? Destroyable
                    ?: throw RuntimeException("RecyclerView must use " +
                            "destroyable RecycledViewPool, otherwise memory leaks will occur. " +
                            "See DestroyableRecycledViewPool.")
            destroyablePool.destroy()
            recycledViewPool.clear()
        }
    }

    rawLifecycleOwner?.lifecycle?.forceMarkState(Lifecycle.State.DESTROYED)

    companionFragment?.destroyed = true

    tag = ViewDestroyed
}

fun ViewGroup.removeAndDestroyView(v: View) {
    if (indexOfChild(v) >= 0) {
        v.destroy()
    }
    removeView(v)
}

fun ViewGroup.removeAndDestroyViewAt(index: Int) {
    getChildAt(index)?.destroy()
    removeViewAt(index)
}

fun ViewGroup.removeAndDestroyViews(start: Int, count: Int) {
    for (i in start until start + count) {
        getChildAt(i)?.destroy()
    }
    removeViews(start, count)
}

fun ViewGroup.removeAndDestroyAllViews() {
    removeAndDestroyViews(0, childCount)
}

internal val View.activity: FragmentActivity
    get() {
        var c = context
        while (c !is FragmentActivity && c is ContextWrapper) {
            c = c.baseContext
        }

        return c as? FragmentActivity
                ?: throw IllegalStateException("Could not find FragmentActivity for $this.")
    }

internal fun View.getOrCreateCompanionFragment(): ViewCompanionFragment {
    return companionFragment
            ?: ViewCompanionFragment().also {
                if (id == View.NO_ID) {
                    if (!checkLifecycleState(Lifecycle.State.CREATED)) {
                        throw IllegalStateException("View doesn't have an id and " +
                                "lifecycle is not in created state yet.")
                    }
                    id = ViewCompat.generateViewId()
                }

                it.owningView = this
                activity.supportFragmentManager
                        .beginTransaction()
                        .add(it, companionFragmentTag)
                        .commitNowAllowingStateLoss()
            }
}

internal fun View.checkLifecycleState(state: Lifecycle.State): Boolean {
    val currentState = rawLifecycleOwner?.lifecycle?.currentState
    return currentState?.isAtLeast(state) == true
}

internal val View.companionFragment: ViewCompanionFragment?
    get() = activity.supportFragmentManager.findFragmentByTag(companionFragmentTag)
            as? ViewCompanionFragment

internal val View.companionFragmentTag: String
    get() = "Companion fragment for ${javaClass.canonicalName} : $id"

internal val View.isDisplayed: Boolean
    get() = ViewCompat.isAttachedToWindow(this) && visibility != View.GONE

internal object ViewDestroyed