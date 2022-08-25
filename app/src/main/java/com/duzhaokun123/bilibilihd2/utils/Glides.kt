package com.duzhaokun123.bilibilihd2.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.duzhaokun123.bilibilihd2.R
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation

fun glideSafeLoadInto(
    url: String?,
    options: RequestOptions,
    target: ImageView,
    context: Context = application
) {
    if (url == null) {
        target.setImageDrawable(null)
        return
    }

    val placeholder = ColorDrawable(context.getColorCompat(R.color.image_background))

    try {
        Glide.with(context)
            .load(url)
            .apply(options)
            .placeholder(placeholder)
//            .override(Target.SIZE_ORIGINAL)
            .into(target)
//            .into(object : CustomViewTarget<ImageView, Drawable>(target) {
//                override fun onLoadFailed(errorDrawable: Drawable?) {
//                    target.setImageDrawable(placeholder)
//                }
//
//                override fun onResourceCleared(placeholder: Drawable?) {
//                    target.setImageDrawable(placeholder)
//                }
//
//                override fun onResourceReady(
//                    resource: Drawable,
//                    transition: Transition<in Drawable>?
//                ) {
//                    if (target.width < 600.dpToPx()) {
//                        target.setImageDrawable(resource)
//                    } else {
//                        target.post {
//                            // FIXME: not effected
//                            target.setImageDrawable(resource)
//                            System.out.println("---- post setImageDrawable w: " + target.width + ", h: " + target.height + ", over: " + (target.width >= 600.dpToPx()))
//                        }
//                    }
//
//                }
//            })

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun picassoSafeLoadInto(url: String?, target: ImageView, context: Context = application) {
    if (url == null) {
        target.setImageDrawable(null)
        return
    }
    try {
        val transformation = object : Transformation {
            override fun transform(source: Bitmap): Bitmap {
                val targetWidth = target.width
                if (source.width == 0) return source
                val ratio = targetWidth.toFloat() / source.width.toFloat()
                val targetHeight = (source.height * ratio).toInt()

                val result = runCatching {
                    Bitmap.createScaledBitmap(
                        source,
                        targetWidth,
                        targetHeight,
                        false
                    )
                }.getOrDefault(source)
                if (result != source) {
                    source.recycle()
                }
                return result
            }

            override fun key() = "transformation desired width"
        }

        Picasso.get().load(url)
            .placeholder(ColorDrawable(context.getColorCompat(R.color.image_background)))
            .transform(transformation)
            .into(target)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun glideSafeGet(url: String?, onGet: suspend (bitmap: Bitmap) -> Unit) {
    url ?: return
    runNewThread {
        try {
            val bitmap = Glide.with(application).asBitmap().load(url).submit().get()
            runMain {
                onGet(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun glideSafeGetSync(url: String?): Bitmap? {
    url ?: return null
    try {
        return Glide.with(application).asBitmap().load(url).submit().get()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}