package com.duzhaokun123.bilibilihd2.ui.play.live

import android.content.Context
import android.content.Intent
import com.bapis.bilibili.app.view.v1.Live
import com.duzhaokun123.annotationProcessor.IntentFilter
import com.duzhaokun123.bilibilihd2.bases.BasePlayActivity
import com.duzhaokun123.bilibilihd2.ui.UrlOpenActivity
import com.duzhaokun123.bilibilihd2.ui.play.online.OnlinePlayActivity
import com.duzhaokun123.bilibilihd2.utils.bilibiliClient
import com.duzhaokun123.bilibilihd2.utils.runIOCatchingResultRunMain
import com.google.gson.JsonObject
import com.hiczp.bilibili.api.BilibiliClient
import com.hiczp.bilibili.api.live.websocket.LiveClient
import com.hiczp.bilibili.api.live.websocket.liveClient
import io.ktor.http.cio.websocket.*

class LivePlayActivity : BasePlayActivity() {
    companion object {
        private const val EXTRA_CID = "cid"

        @IntentFilter
        class LiveIntentFilter : UrlOpenActivity.IIntentFilter {
            override fun handle(
                parsedIntent: UrlOpenActivity.ParsedIntent,
                context: Context
            ): Pair<Intent?, String?> {
                return when (parsedIntent.host) {
                    "live", "live.bilibili.com" ->
                        Intent(context, LivePlayActivity::class.java).apply {
                            putExtra(EXTRA_CID, parsedIntent.paths[0].toLong())
                        } to "直播 ${parsedIntent.paths[0]}"
                    else -> null to null
                }
            }
        }
    }

    val cid by lazy { startIntent.getLongExtra(LivePlayActivity.EXTRA_CID, 0) }
    lateinit var liveClient: LiveClient

    override fun initData() {
        super.initData()
//        test()
    }

    fun test() {
        liveClient = bilibiliClient.liveClient(cid, doEntryRoomAction = false, sendUserOnlineHeart = true) {
            /**
             * 成功进入房间时触发
             */
            onConnect = { client ->
                println("---- onConnect ${client.roomId}")
            }

            /**
             * 抛出异常时触发
             */
            onError = { client, error ->
                println("---- onError ${client.roomId}, error ${error.message}")
            }

            /**
             * 收到人气值数据包
             */
            onPopularityPacket = { client, num ->
                println("---- onPopularityPacket ${client.roomId}")
            }

            /**
             * 收到 command 数据包
             */
            onCommandPacket = { client, cmd ->
                println("---- onCommandPacket ${client.roomId}")
            }

            /**
             * 连接关闭时触发
             */
            onClose = { client, reason ->
                println("---- onClose ${client.roomId}, reason ${reason?.message ?: "none"}")
            }
        }

        println("---- initData $cid, ${liveClient.roomId}")

//        runIOCatchingResultRunMain(this@LivePlayActivity, {
            liveClient.launch()
//        })
    }
}