package com.viewlifecycle

import android.content.ContextWrapper
import android.graphics.Region
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import java.util.*

/**
 * Obtain a [LifecycleOwner] of the view. View's lifecycle depends on:
 * - the lifecycle of the owning activity
 * - the lifecycle of the parent ViewGroup
 * - the layout position in the parent ViewGroup
 */
@Suppress("unused")
var View.lifecycleOwner: LifecycleOwner by ViewLifecycleOwnerDelegate {
    ensureParentLifecycleDispatcherAttached()
    attachLifecycleOwner()
}
    internal set

private fun View.ensureParentLifecycleDispatcherAttached() {
    onAttached {
        (parent as? ViewGroup)?.attachViewGroupLifecycleDispatcher()
    }
}

private fun View.attachLifecycleOwner(): LifecycleOwner {
    val current = rawLifecycleOwner
    if (current != null) {
        return current
    }

    // Special case when navigate methods are called multiple times (i.e. to form a back stack).
    // View is created, added and immediately removed in a ViewGroup,
    // not being attached to a window, so we need to quickly clear memory associated with it.
    if (!ViewCompat.isAttachedToWindow(this)) {
        mainHandler.postDelayed({
            if (!ViewCompat.isAttachedToWindow(this)) {
                destroy()
            }
        }, 100)
    }

    return ViewLifecycleOwner(this).also {
        rawLifecycleOwner = it
    }
}

internal fun View.detachLifecycleOwner() {
    lifecycleOwner = ViewLifecycleOwnerDelegate.NullLifecycleOwner
    rawLifecycleOwner = null
}

internal fun ViewGroup.attachViewGroupLifecycleDispatcher() {
    if (viewGroupLifecycleDispatcher != null) {
        return
    }

    attachLifecycleOwner()
    viewGroupLifecycleDispatcher = ViewGroupLifecycleDispatcher(this)
}

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

/**
 * Returns a property delegate to access [ViewModel] by **default** scoped to this [View]:
 * ```
 * class MyView : View {
 *     val viewmodel: MYViewModel by viewModels()
 * }
 * ```
 *
 * Custom [ViewModelProvider.Factory] can be defined via [factoryProducer] parameter,
 * factory returned by it will be used to create [ViewModel]:
 * ```
 * class MyView : View() {
 *     val viewmodel: MYViewModel by viewModels { myFactory }
 * }
 * ```
 *
 * Default scope may be overridden with parameter [viewModelScope]:
 * ```
 * class MyView : View() {
 *     val viewmodel: MYViewModel by viewModels ({parent as View})
 * }
 * ```
 */
@MainThread
inline fun <reified VM : ViewModel> View.viewModels(
    noinline viewModelScope: () -> View = { this },
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val storeProducer = {
        ViewCompanionFragment.getOrCreate(viewModelScope()).viewModelStore
    }
    val factoryPromise = factoryProducer ?: {
        ViewCompanionFragment.getOrCreate(this).defaultViewModelProviderFactory
    }
    return ViewModelLazy(VM::class, storeProducer, factoryPromise)
}

/**
 * Access arguments associated with a View.
 * The arguments are retained across configuration changes.
 */
@Suppress("unused")
var View.arguments: Bundle? by FragmentHolderDelegate()

internal fun View.destroy() {
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).destroy()
        }
    }

    updateState(Lifecycle.State.DESTROYED)
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

internal var ViewGroup.viewGroupLifecycleDispatcher: ViewGroupLifecycleDispatcher?
        by DispatcherHolderDelegate()

internal var View.hierarchyLifecycleDispatcher: HierarchyLifecycleDispatcher? by HolderDelegate()

internal var View.level: Int
    get() = getTag(R.id.level) as? Int ?: 0
    set(value) = setTag(R.id.level, value)

internal var View.visibleRegion: Region?
    get() = getTag(R.id.region) as? Region
    set(value) = setTag(R.id.region, value)

internal var View.isBackStackItem: Boolean
    get() = getTag(R.id.backStackItem) == true
    set(value) = setTag(R.id.backStackItem, value)

internal var View.isDisplayedTag: Boolean?
    get() = getTag(R.id.displayed) as? Boolean
    set(value) = setTag(R.id.displayed, value)

internal var View.visibilityTag: Int?
    get() = getTag(R.id.visibility) as? Int
    set(value) = setTag(R.id.visibility, value)

var View.isReusable: Boolean
    get() = getTag(R.id.reusable) == true
    set(value) {
        setTag(R.id.reusable, value)
        if (!value) {
            if (rawLifecycleOwner?.lifecycle?.currentState == Lifecycle.State.CREATED) {
                destroy()
            }
        }
    }

internal fun View.updateState(state: Lifecycle.State) {
    if (!state.isAtLeast(Lifecycle.State.STARTED) || level == 0 && isDisplayed) {
        rawLifecycleOwner?.lifecycle?.currentState = state
    } else {
        rawLifecycleOwner?.lifecycle?.currentState = Lifecycle.State.CREATED
    }
}

internal val View.isDisplayed: Boolean
    get() {
        if (!ViewCompat.isAttachedToWindow(this)) {
            return false
        }
        return innerStem.plus(this).all {
            it.visibility != View.GONE
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

internal val View.navCompanionFragmentTag: String
    get() = "NavViewCompanionFragment for ${javaClass.canonicalName} : $id"

internal val View.companionFragmentTag: String
    get() = "ViewCompanionFragment for ${javaClass.canonicalName} : $id"

internal inline fun <T : View> T.onAttached(crossinline callback: T.() -> Unit) {
    if (ViewCompat.isAttachedToWindow(this)) {
        callback()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                removeOnAttachStateChangeListener(this)
            }

            override fun onViewAttachedToWindow(v: View?) {
                removeOnAttachStateChangeListener(this)

                callback()
            }
        })
    }
}

private val mainHandler = Handler(Looper.getMainLooper())

fun View.debugViewLifecycle() {
    isDisplayedTag = isDisplayed
    visibilityTag = visibility
    viewTreeObserver.addOnGlobalLayoutListener {
        val _isDisplayed = isDisplayed
        if (rawLifecycleOwner != null && isDisplayedTag != _isDisplayed) {
            isDisplayedTag = _isDisplayed
            if (visibilityTag == visibility) {
                if (_isDisplayed) {
                    innerStem.forEach {
                        if (it.visibility == View.VISIBLE && (it.parent as? ViewGroup)?.viewGroupLifecycleDispatcher == null) {
                            Log.v("ViewLifecycle", "$this became visible on screen, " +
                                    "but its visibility property was not changed, " +
                                    "so if it did not receive appropriate lifecycle event, " +
                                    "look for its parent that became visible and call View.lifecycleOwner on it.")
                        }
                    }
                } else {
                    innerStem.forEach {
                        if (it.visibility == View.GONE && (it.parent as? ViewGroup)?.viewGroupLifecycleDispatcher == null) {
                            Log.w("ViewLifecycle", "$it became GONE, call View.lifecycleOwner on it so " +
                                    "it can affect the lifecycle of $this.")
                        }
                    }
                }
            }
            visibilityTag = visibility
        }
    }
}
