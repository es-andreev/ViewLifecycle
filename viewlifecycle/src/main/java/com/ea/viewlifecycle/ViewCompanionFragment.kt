package com.ea.viewlifecycle

import android.arch.lifecycle.ViewModel
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import android.view.ViewGroup
import java.io.Serializable

/**
 * ViewCompanionFragment is a helper fragment and is bound to the owning View -
 * conversely to a fragments framework, where a View is an internal part of a Fragment.
 *
 * It's used:
 * - to access [ViewModel]s for views across configuration changes.
 * - to restore navigation into a [owningView], if such a view is found after configuration change.
 *
 * Unique per pair [View.javaClass] + [View.getId].
 */
class ViewCompanionFragment : Fragment() {

    private var owningViewId: Int = View.NO_ID
    internal var owningView: View? = null
        set(value) {
            owningViewId = value?.id ?: View.NO_ID
            field = value
        }

    internal var destroyed: Boolean = false
        set(value) {
            if (value) {
                owningView = null
                activity?.supportFragmentManager?.beginTransaction()
                        ?.remove(this)?.commitNowAllowingStateLoss()
            }
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            destroyed = savedInstanceState.getBoolean(STATE_DESTROYED)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            owningViewId = savedInstanceState.getInt(STATE__VIEW_ID)
            if (owningViewId != View.NO_ID) {
                owningView = activity?.findViewById(owningViewId)
            }

            if (owningView == null) {
                destroyed = true
            } else if (owningView is ViewGroup) {
                val viewGroup = owningView as ViewGroup
                val stack = stackFromBundle(savedInstanceState)
                if (stack != null) {
                    stack.forEach {
                        if (viewGroup.findViewById<View>(it.id) == null) {
                            val viewClass = Class.forName(it.viewClassName)
                            val view = viewClass.getConstructor(Context::class.java)
                                    .newInstance(activity) as View
                            view.id = it.id
                            view.arguments = it.args

                            viewGroup.addView(view)
                        }
                    }
                    viewGroup.attachNavigation()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (owningView?.rawLifecycleOwner?.lifecycle?.dispatcher != null) {
            val viewGroup = owningView as ViewGroup
            stackToBundle(viewGroup, outState)
        }

        outState.putInt(STATE__VIEW_ID, owningViewId)
        outState.putBoolean(STATE_DESTROYED, destroyed)
        super.onSaveInstanceState(outState)
    }

    private fun stackFromBundle(bundle: Bundle): ArrayList<StackData>? {
        @Suppress("UNCHECKED_CAST")
        return bundle.getSerializable(STATE_STACK) as? ArrayList<StackData>
    }

    private fun stackToBundle(viewGroup: ViewGroup, bundle: Bundle) {
        val stackData = ArrayList<StackData>()
        for (i in 0 until viewGroup.childCount) {
            val view = viewGroup.getChildAt(i)
            stackData.add(StackData.of(view))
        }
        bundle.putSerializable(STATE_STACK, stackData)
    }

    companion object {
        private const val STATE__VIEW_ID = "viewId"
        private const val STATE_DESTROYED = "destroyed"
        private const val STATE_STACK = "stack"
    }

    private data class StackData(val viewClassName: String, val id: Int, val args: Bundle?) : Serializable {
        companion object {
            fun of(view: View) = StackData(view::class.java.canonicalName
                    ?: throw RuntimeException("View must be a top level class."), view.id, view.arguments)
        }
    }
}