package com.ea.viewlifecycle

import android.support.annotation.CallSuper
import java.util.*
import kotlin.reflect.KProperty

internal open class HolderDelegate<E, T> {

    protected val values = WeakHashMap<E, T?>()

    open operator fun getValue(thisRef: E, property: KProperty<*>): T? = synchronized(values) {
        return values[thisRef]
    }

    @CallSuper
    open operator fun setValue(thisRef: E, property: KProperty<*>, value: T?) {
        if (value == null) {
            values.remove(thisRef)
        } else {
            values[thisRef] = value
        }
    }
}