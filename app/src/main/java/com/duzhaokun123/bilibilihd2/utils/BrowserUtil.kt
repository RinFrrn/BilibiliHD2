package com.duzhaokun123.bilibilihd2.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.duzhaokun123.bilibilihd2.R
import com.duzhaokun123.bilibilihd2.ui.UrlOpenActivity
import com.duzhaokun123.bilibilihd2.ui.WebViewActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog
import intentFilters
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil

object BrowserUtil {
    private const val TAG = "BrowserUtil"

    fun openInApp(
        context: Context?, url: String?, openDirectly: Boolean = true
    ) {
        if (context == null || url == null) return
        try {
//            val openDirectly = true  // 直接打开播放页
            if (openDirectly) {
                openInAppDirectly(context, Uri.parse(url))
            } else {
                val intent = Intent(context, UrlOpenActivity::class.java).apply {
                    data = Uri.parse(url)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            TipUtil.showToast(e.message)
        }
    }

    private fun openInAppDirectly(
        context: Context, uri: Uri
    ) {
        Analytics.trackEvent("open url directly")
        val validIntents = UrlOpenActivity.getValidIntents(context, uri)
        if (validIntents.isEmpty()) {
//                    text = "不支持此链接"
            Crashes.trackError(
                Exception(), mapOf("issue" to "open unsupported url"),
                listOf(ErrorAttachmentLog.attachmentWithText(uri.toString(), "uri"))
            )
        } else {
            if (validIntents.size == 1) {
                validIntents.first().let {
                    context.startActivity(it.first)
                }
            } else {
                // 跳转至UrlOpenActivity
                openInApp(context, uri.toString(), false)

//                val first = validIntents[0]
//                val second = validIntents[1]
//
//                MaterialAlertDialogBuilder(context)
//                    .setTitle("Open with...")
//                    .setNeutralButton("Cancel") { dialog, which ->
//                        dialog.dismiss()
//                    }
//                    .setNegativeButton(first.second ?: first.first.toString()) { dialog, which ->
//                        context.startActivity(first.first)
//                    }
//                    .setPositiveButton(second.second ?: second.first.toString()) { dialog, which ->
//                        context.startActivity(second.first)
//                    }
//                    .show()
            }
        }
    }

    fun openWebViewActivity(
        context: Context?,
        url: String?,
        ua: WebViewActivity.Companion.UA = WebViewActivity.Companion.UA.TABLET,
        interceptAll: Boolean = false,
        finishWhenIntercept: Boolean = false
    ) {
        if (url != null)
            openWebViewActivity(context, url.toUri(), ua, interceptAll, finishWhenIntercept)
    }

    fun openWebViewActivity(
        context: Context?,
        uri: Uri,
        ua: WebViewActivity.Companion.UA = WebViewActivity.Companion.UA.TABLET,
        interceptAll: Boolean = false,
        finishWhenIntercept: Boolean = false
    ) {
        if (context == null) return
        val intent = Intent(context, WebViewActivity::class.java)
        intent.data = uri
        intent.putExtra(WebViewActivity.EXTRA_UA, ua)
        intent.putExtra(WebViewActivity.EXTRA_INTERCEPT_ALL, interceptAll)
        intent.putExtra(WebViewActivity.EXTRA_FINISH_WHEN_INTERCEPT, finishWhenIntercept)
        context.startActivity(intent)
    }

    fun openCustomTab(context: Context?, url: String) {
        Log.d(TAG, "openCustomTab: openUrl = $url")
        if (context == null) {
            Log.d(TAG, "openCustomTab: but context == null")
            return
        }
        try {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, Uri.parse(url))
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    fun syncLoginResponseCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        val loginResponse = bilibiliClient.loginResponse
        cookieManager.removeAllCookies(null)
        if (loginResponse == null) {
            return
        }
        for (url in loginResponse.data.cookieInfo.domains) {
            for ((_, /* _, */ name, value) in loginResponse.data.cookieInfo.cookies) {
                cookieManager.setCookie(url, "$name=$value")
            }
            cookieManager.setCookie(url, "Domain=$url")
            cookieManager.setCookie(url, "Path=/")
        }
        cookieManager.flush()
    }
}