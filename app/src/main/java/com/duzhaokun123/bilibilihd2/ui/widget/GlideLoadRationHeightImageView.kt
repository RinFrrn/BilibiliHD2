package com.duzhaokun123.bilibilihd2.ui.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.res.use
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.duzhaokun123.bilibilihd2.R
import com.duzhaokun123.bilibilihd2.utils.dpToPx

class GlideLoadRationHeightImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : GlideLoadImageView(context, attrs, defStyleAttr) {
    init {
        context.obtainStyledAttributes(attrs, R.styleable.Ration, defStyleAttr, 0).use {
            ration = it.getFloat(R.styleable.Ration_ration, 0F)
        }
    }

    var ration: Float = 0F
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    override val options: RequestOptions
        get() = RequestOptions()
            .centerCrop()
//            .transform(CenterCrop(), RoundedCorners(6.dpToPx()))

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (ration > 0) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = (width * ration).toInt()

//            setMeasuredDimension(width, height)
            super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
            )
        } else
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}