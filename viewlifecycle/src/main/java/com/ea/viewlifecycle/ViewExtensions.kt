package com.ea.viewlifecycle

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import java.util.concurrent.atomic.AtomicInteger

/**
 * Obtain a [LifecycleOwner] of the view. View's lifecycle depends on:
 * - the lifecycle of the owning activity
 * - whether it's attached to a window (items in a dynamic ViewGroups like RecyclerView
 * can have a proper lifecycle)
 * - the layout position in the parent ViewGroup if it is a navigation container,
 * i.e. [attachNavigation] was called on it.
 *
 * If you are going to remove the view, don't forget to destroy it,
 * see [ViewLifecycleOwner] for explanation.
 */
var View.lifecycleOwner: LifecycleOwner by LazyLifecycleOwnerDelegate {
    ViewLifecycleOwner(this).also {
        rawLifecycleOwner = it
    }
}
    internal set

/**
 * Mark a [ViewGroup] as a navigation container. You can add and remove views in it without
 * worrying about destroying them. Lifecycle state is propagated to the children appropriately.
 * After a configuration change, if a ViewGroup with the same id is found in the hierarchy,
 * all its direct children will be restored. See [ViewLifecycleDispatcher].
 */
fun ViewGroup.attachNavigation() {
    if (viewLifecycleDispatcher != null) {
        // already attached
        return
    }

    post {
        lifecycleOwner // ensure ViewLifecycleOwner is initialised
        ViewCompanionFragment.getOrCreate(this) // ensure ViewCompanionFragment is attached

        ViewLifecycleDispatcher(this).apply {
            viewLifecycleDispatcher = this
            attach()
            requestLayout()
        }
    }
}

/**
 * Detach navigation for convenience.
 */
fun ViewGroup.detachNavigation() {
    viewLifecycleDispatcher?.detach()
    hierarchyLifecycleDispatcher?.detach()
}

val View.viewModelProvider: ViewModelProvider
    get() = viewModelProvider(null)

fun View.viewModelProvider(factory: ViewModelProvider.Factory?): ViewModelProvider {
    if (rawLifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.CREATED) != true) {
        throw IllegalStateException("Cannot create ViewModelProvider until " +
                "LifecycleOwner is in created state.")
    }
    return ViewModelProviders.of(ViewCompanionFragment.getOrCreate(this), factory)
}

var View.arguments: Bundle? by HolderDelegate()

fun View.destroy() {
    if (getTag(id) === ViewDestroyed) {
        return
    }

    if (this is ViewGroup) {
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

    ViewCompanionFragment.get(this)?.destroyed = true

    setTag(id, ViewDestroyed)
}

fun ViewGroup.removeAndDestroyView(v: View) {
    val index = indexOfChild(v)
    if (index >= 0) {
        removeAndDestroyViewAt(index)
    }
}

fun ViewGroup.removeAndDestroyViewAt(index: Int) {
    removeAndDestroyViews(index, 1)
}

fun ViewGroup.removeAndDestroyAllViews() {
    removeAndDestroyViews(0, childCount)
}

fun ViewGroup.removeAndDestroyViews(start: Int, count: Int) {
    for (i in start until start + count) {
        getChildAt(i)?.destroy()
    }
    removeViews(start, count)
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

internal val View.root: ViewGroup
    get() {
        var parent: View = this
        while (parent.parent is ViewGroup && (parent.parent as View).context is FragmentActivity) {
            parent = parent.parent as ViewGroup
        }
        return parent as ViewGroup
    }

internal var View.rawLifecycleOwner: ViewLifecycleOwner? by HolderDelegate()

internal var View.viewLifecycleDispatcher: ViewLifecycleDispatcher? by DispatcherHolderDelegate()

internal var View.hierarchyLifecycleDispatcher: HierarchyLifecycleDispatcher? by HolderDelegate()

internal val View.isDisplayed: Boolean
    get() = ViewCompat.isAttachedToWindow(this) && visibility != View.GONE

internal object ViewDestroyed

private val sNextGeneratedId = AtomicInteger(1)
// copy-paste from ViewCompat for old support library versions
internal fun generateViewId(): Int {
    @Suppress("LiftReturnOrAssignment")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        return View.generateViewId()
    } else {
        var result: Int
        var newValue: Int
        do {
            result = sNextGeneratedId.get()
            newValue = result + 1
            if (newValue > 16777215) {
                newValue = 1
            }
        } while (!sNextGeneratedId.compareAndSet(result, newValue))

        return result
    }
}