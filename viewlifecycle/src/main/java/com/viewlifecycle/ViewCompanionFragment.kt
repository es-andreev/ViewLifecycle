package com.viewlifecycle

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel

/**
 * ViewCompanionFragment is used:
 * - to obtain [ViewModel]s for views and retain them across configuration changes.
 * - to access arguments.
 * Unique per pair [View.javaClass] + [View.getId].
 */
class ViewCompanionFragment : Fragment() {

    companion object {
        fun getOrCreate(view: View): ViewCompanionFragment {
            return get(view)
                    ?: ViewCompanionFragment().also {
                        view.activity.supportFragmentManager
                                .beginTransaction()
                                .add(it, view.companionFragmentTag)
                                .commitNowAllowingStateLoss()
                    }
        }

        internal fun get(view: View): ViewCompanionFragment? {
            return view.activity.supportFragmentManager
                    .findFragmentByTag(view.companionFragmentTag)
                    as? ViewCompanionFragment
        }
    }
}