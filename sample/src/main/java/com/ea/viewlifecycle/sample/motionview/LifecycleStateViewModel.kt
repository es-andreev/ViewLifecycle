package com.ea.viewlifecycle.sample.motionview

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.widget.FrameLayout

class LifecycleStateViewModel : ViewModel() {

    val liveLayoutParams = MutableLiveData<FrameLayout.LayoutParams>()
    val liveTranslationX = MutableLiveData<Float>()
    val liveTranslationY = MutableLiveData<Float>()
}