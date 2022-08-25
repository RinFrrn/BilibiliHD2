package com.duzhaokun123.bilibilihd2.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.request.RequestOptions
import com.duzhaokun123.bilibilihd2.utils.glideSafeLoadInto

open class GlideLoadImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    open val options: RequestOptions get() = RequestOptions()

    fun setImageUrl(url: String?) {
        glideSafeLoadInto(url, options, this, context)
    }
}