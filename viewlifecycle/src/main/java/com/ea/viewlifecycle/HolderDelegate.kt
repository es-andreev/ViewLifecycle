package com.ea.viewlifecycle

import android.support.annotation.CallSuper
import java.util.*
import kotlin.reflect.KProperty

internal open class HolderDelegate<T> {

    protected val values = WeakHashMap<Any, T?>()

    operator fun getValue(thisRef: Any, property: KProperty<*>): T? = synchronized(values) {
        return values[thisRef]
    }

    @CallSuper
    open operator fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        if (value == null) {
            values.remove(thisRef)
        } else {
            values[thisRef] = value
        }
    }
}