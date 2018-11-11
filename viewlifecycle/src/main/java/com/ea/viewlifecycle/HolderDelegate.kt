package com.ea.viewlifecycle

import android.support.annotation.CallSuper
import android.view.View
import java.util.*
import kotlin.reflect.KProperty

internal open class HolderDelegate<T> {

    protected val values = WeakHashMap<Any, T?>()

    operator fun getValue(thisRef: View, property: KProperty<*>): T? = synchronized(values) {
        return values[thisRef]
    }

    @CallSuper
    open operator fun setValue(thisRef: View, property: KProperty<*>, value: T?) {
        if (value == null) {
            values.remove(thisRef)
        } else {
            values[thisRef] = value
        }
    }
}