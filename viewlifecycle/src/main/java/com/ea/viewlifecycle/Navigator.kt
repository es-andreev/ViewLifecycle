package com.ea.viewlifecycle

import android.view.View

/**
 * Provides methods for navigating between views.
 */
interface Navigator {

    /**
     * Adds a [view] and removes the current putting it into a back stack.
     *
     * @param view The [View] to add.
     */
    fun navigateForward(view: View)

    /**
     * Adds a [view] and keeps the current one. Navigating back will just remove it.
     *
     * @param view The [View] to add.
     */
    fun navigateAdd(view: View)

    /**
     * If there is a back stack, removes the current view and restores the previous.
     *
     * @return true if the previous view was restored, false otherwise.
     */
    fun navigateBack(): Boolean

    /**
     * Restore navigation state to a [className] view.
     *
     * @return true if the [className] view was restored, false otherwise.
     */
    fun navigateBackTo(className: String): Boolean

    /**
     * Restore navigation state that was before [className] view.
     *
     * @return true if the view before [className] view was restored, false otherwise.
     */
    fun navigateBackIncluding(className: String): Boolean

    /**
     * Adds a [view] and removes the current one. Does not change the back stack.
     */
    fun navigateReplace(view: View)

    /**
     * Adds a [view], removes all current views and drops the whole back stack.
     */
    fun navigateReplaceAll(view: View)
}