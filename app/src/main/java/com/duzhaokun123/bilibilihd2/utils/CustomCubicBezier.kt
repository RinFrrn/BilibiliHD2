package com.duzhaokun123.bilibilihd2.utils

import android.view.animation.Interpolator
import androidx.core.view.animation.PathInterpolatorCompat

/** A custom cubic bezier interpolator which exposes its control points.  */
class CustomCubicBezier constructor(
    val controlX1: Float,
    val controlY1: Float,
    val controlX2: Float,
    val controlY2: Float
) : Interpolator {
    companion object {
        fun smoothSheetInterpolator() = CustomCubicBezier(
            0.3f,
            0.9f,
            0.16f,
            1.0f
        )
    }

    private val interpolator: Interpolator = PathInterpolatorCompat.create(
        controlX1,
        controlY1,
        controlX2,
        controlY2
    )

    override fun getInterpolation(input: Float): Float {
        return interpolator.getInterpolation(input)
    }

//    fun getDescription(context: Context): String {
//        return context.getString(
//            "R.string.cat_transition_config_custom_interpolator_desc",
//            controlX1,
//            controlY1,
//            controlX2,
//            controlY2
//        )
//    }
}