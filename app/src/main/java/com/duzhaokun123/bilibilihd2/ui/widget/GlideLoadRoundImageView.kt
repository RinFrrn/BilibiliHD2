package com.duzhaokun123.bilibilihd2.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.duzhaokun123.bilibilihd2.utils.dpToPx

open class GlideLoadRoundImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : GlideLoadImageView(context, attrs, defStyleAttr) {

    override val options: RequestOptions
        get() = RequestOptions()
            .transform(CenterCrop(), CircleCrop())

//    override fun setImageDrawable(drawable: Drawable?) {
//        if (drawable == null) {
//            super.setImageDrawable(drawable)
//            return
//        }
////        var width = drawable.intrinsicWidth
////        if (width <= 0) width = 1
////        var height = drawable.intrinsicHeight
////        if (height <= 0) height = 1
//
//        val width = 50.dpToPx()
//        val height = 50.dpToPx()
//
//        val bitmap = drawable.toBitmap(width, height)
//        val roundedBitmapDrawable =
//            RoundedBitmapDrawableFactory.create(context.resources, bitmap).apply {
//                setAntiAlias(true)
//                isCircular = true
//                scaleType = ScaleType.CENTER_CROP
//            }
//        super.setImageDrawable(roundedBitmapDrawable)
//    }

//    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
//        // Raw height and width of image
//        val (height: Int, width: Int) = options.run { outHeight to outWidth }
//        var inSampleSize = 1
//
//        if (height > reqHeight || width > reqWidth) {
//
//            val halfHeight: Int = height / 2
//            val halfWidth: Int = width / 2
//
//            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
//            // height and width larger than the requested height and width.
//            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
//                inSampleSize *= 2
//            }
//        }
//
//        return inSampleSize
//    }
//
//
//    fun decodeSampledBitmapFromResource(
//        res: Resources,
//        resId: Int,
//        reqWidth: Int,
//        reqHeight: Int
//    ): Bitmap {
//        // First decode with inJustDecodeBounds=true to check dimensions
//        return BitmapFactory.Options().run {
//            inJustDecodeBounds = true
//            BitmapFactory.decodeResource(res, resId, this)
//
//            // Calculate inSampleSize
//            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
//
//            // Decode bitmap with inSampleSize set
//            inJustDecodeBounds = false
//
//            BitmapFactory.decodeResource(res, resId, this)
//        }
//    }

}