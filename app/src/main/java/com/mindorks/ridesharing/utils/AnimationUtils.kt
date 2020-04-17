package com.mindorks.ridesharing.utils

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

object AnimationUtils {
    fun polyLineAnimator(): ValueAnimator{
        val valueAnimator = ValueAnimator.ofInt(0, 100)
        valueAnimator.run {
            interpolator = LinearInterpolator()
            duration = 2000
        }
        return valueAnimator
    }
}