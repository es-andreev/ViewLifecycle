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
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

// TODO update doc

/**
 * Obtain a [LifecycleOwner] of the view. View's lifecycle depends on:
 * - the lifecycle of the owning activity
 * - whether it's attached to a window (items in a dynamic ViewGroups like RecyclerView
 * can have a proper lifecycle)
 * - the layout position in the parent ViewGroup if it is a navigation container,
 * i.e. [attachViewGroupLifecycleDispatcher] was called on it.
 *
 * If you are going to remove the view, don't forget to destroy it,
 * see [ViewLifecycleOwner] for explanation.
 */
@Suppress("unused")
var View.lifecycleOwner: LifecycleOwner by ViewLifecycleOwnerDelegate {
    ensureParentLifecycleDispatcherAttached()
    attachLifecycleOwner()
}
    internal set

private fun View.ensureParentLifecycleDispatcherAttached() {
    if (parent == null) {
        afterMeasured {
            (parent as? ViewGroup)?.attachViewGroupLifecycleDispatcher()
        }
    } else {
        (parent as? ViewGroup)?.attachViewGroupLifecycleDispatcher()
    }
}

private fun View.attachLifecycleOwner(): LifecycleOwner {
    val current = rawLifecycleOwner
    if (current != null) {
        return current
    }

    setTag(R.id.view_destroyed, null)
    return ViewLifecycleOwner(this).also {
        rawLifecycleOwner = it
    }
}

internal fun View.detachLifecycleOwner() {
    lifecycleOwner = ViewLifecycleOwnerDelegate.NullLifecycleOwner
    rawLifecycleOwner = null
}

fun ViewGroup.trackNavigation(track: Boolean = true) {
    setTag(R.id.viewGroup_navigator, if (track) ViewGroupNavigator else null)
    if (track) {
        ensureParentLifecycleDispatcherAttached()

        // ensure ViewCompanionFragment is attached
        ViewCompanionFragment.getOrCreate(this)
    }
}

internal val ViewGroup.isTrackingNavigation: Boolean
    get() = getTag(R.id.viewGroup_navigator) === ViewGroupNavigator

/**
 * Mark a [ViewGroup] as a navigation container. You can add and remove views in it without
 * worrying about destroying them. Lifecycle state is propagated to the children appropriately.
 * After a configuration change, if a ViewGroup with the same id is found in the hierarchy,
 * all its direct children will be restored. See [ViewGroupLifecycleDispatcher].
 */
internal fun ViewGroup.attachViewGroupLifecycleDispatcher() {
    if (viewGroupLifecycleDispatcher != null) {
        return
    }

    post {
        attachLifecycleOwner()

        ViewGroupLifecycleDispatcher(this).apply {
            viewGroupLifecycleDispatcher = this
            requestLayout()
        }
    }
}

/**
 * Detach navigation for convenience.
 */
internal fun ViewGroup.detachViewGroupLifecycleDispatcher() {
    val dispatcher = viewGroupLifecycleDispatcher
    if (dispatcher != null) {
        dispatcher.clear()
        viewGroupLifecycleDispatcher = null
    }
}

internal fun ViewGroup.attachHierarchyLifecycleDispatcher() {
    if (hierarchyLifecycleDispatcher != null) {
        return
    }

    attachLifecycleOwner()
    hierarchyLifecycleDispatcher = HierarchyLifecycleDispatcher(this)
}

internal fun ViewGroup.detachHierarchyLifecycleDispatcher() {
    val dispatcher = hierarchyLifecycleDispatcher
    if (dispatcher != null) {
        dispatcher.clear()
        hierarchyLifecycleDispatcher = null
    }
}

@Suppress("unused")
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

internal fun View.destroy() {
    if (getTag(R.id.view_destroyed) === ViewDestroyed) {
        return
    }

    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).destroy()
        }
    }

    rawLifecycleOwner?.lifecycle?.markState(Lifecycle.State.DESTROYED)

    ViewCompanionFragment.get(this)?.destroyed = true

    setTag(R.id.view_destroyed, ViewDestroyed)
}

internal val View.safeActivity: FragmentActivity?
    get() {
        var c = context
        while (c !is FragmentActivity && c is ContextWrapper) {
            c = c.baseContext
        }

        return c as? FragmentActivity
    }

internal val View.activity: FragmentActivity
    get() = safeActivity
            ?: throw IllegalStateException("Could not find FragmentActivity for $this.")

internal val View.safeRoot: ViewGroup?
    get() {
        var p = if (this is ViewGroup && parent != null) {
            this
        } else {
            parent as? ViewGroup
        }
        while (p?.parent is ViewGroup && (p.parent as View).safeActivity != null) {
            p = p.parent as ViewGroup
        }
        return p
    }

internal var View.rawLifecycleOwner: ViewLifecycleOwner? by HolderDelegate()

internal var View.viewGroupLifecycleDispatcher: ViewGroupLifecycleDispatcher? by DispatcherHolderDelegate()

internal var View.hierarchyLifecycleDispatcher: HierarchyLifecycleDispatcher? by HolderDelegate()

internal val View.isDisplayed: Boolean
    get() = ViewCompat.isAttachedToWindow(this) && visibility != View.GONE

internal object ViewDestroyed

internal object ViewGroupNavigator

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

internal val View.innerStem: ArrayList<ViewGroup>
    get() {
        val parents = ArrayList<ViewGroup>()
        (this as? ViewGroup)?.apply {
            parents.add(this)
        }
        var p: ViewGroup? = parent as? ViewGroup
        val r = safeRoot
        while (p is ViewGroup) {
            if (p === r) {
                break
            }
            parents.add(p)
            p = p.parent as? ViewGroup
        }
        return parents
    }

internal val View.fullStem: ArrayList<ViewGroup>
    get() {
        val parents = ArrayList<ViewGroup>()
        parents.addAll(innerStem)
        safeRoot?.apply {
            parents.add(this)
        }
        return parents
    }

internal inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else {
                    @Suppress("DEPRECATION")
                    viewTreeObserver.removeGlobalOnLayoutListener(this)
                }

                f()
            }
        }
    })
}