package com.duzhaokun123.bilibilihd2.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity

class VibratorUtil constructor(context: Context) {

    // get the VIBRATOR_SERVICE system service
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(AppCompatActivity.VIBRATOR_SERVICE) as Vibrator
        }
    }

    open fun vibrate(effectId: Int) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        // create vibrator effect with a constant
        val effect = VibrationEffect.createPredefined(effectId)

        // it is safe to cancel other vibrations currently taking place
        vibrator.cancel()
        vibrator.vibrate(effect)
//        }
    }
}