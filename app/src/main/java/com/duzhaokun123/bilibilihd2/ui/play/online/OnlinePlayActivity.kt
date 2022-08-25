package com.duzhaokun123.bilibilihd2.ui.play.online

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.*
import android.util.Log
import android.util.Rational
import android.view.*
import android.view.View.OnTouchListener
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RadioButton
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.duzhaokun123.annotationProcessor.IntentFilter
import com.duzhaokun123.bilibilihd2.R
import com.duzhaokun123.bilibilihd2.bases.BasePlayActivity
import com.duzhaokun123.bilibilihd2.databinding.LayoutOnlineplayIntroBinding
import com.duzhaokun123.bilibilihd2.ui.UrlOpenActivity
import com.duzhaokun123.bilibilihd2.ui.comment.RootCommentFragment
import com.duzhaokun123.bilibilihd2.utils.*
import com.duzhaokun123.biliplayer.model.PlayInfo
import com.duzhaokun123.danmakuview.interfaces.DanmakuParser
import com.duzhaokun123.generated.Settings
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.hiczp.bilibili.api.player.model.VideoPlayUrl
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil
import io.github.duzhaokun123.androidapptemplate.utils.maxSystemBarsDisplayCutoutIme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import com.hiczp.bilibili.api.app.model.View as BiliView


class OnlinePlayActivity : BasePlayActivity() {
    companion object {
        private const val EXTRA_AID = "aid"

        @IntentFilter
        class VideoIntentHandler : UrlOpenActivity.IIntentFilter {
            override fun handle(
                parsedIntent: UrlOpenActivity.ParsedIntent,
                context: Context
            ): Pair<Intent?, String?> {
                if (parsedIntent.host !in arrayOf("video", "story")) return null to null
                val p1 = parsedIntent.paths.getOrElse(0) { "0" }
                val aid = try {
                    p1.toLong()
                } catch (e: Exception) {
                    p1.toAid()
                }
                return Intent(context, OnlinePlayActivity::class.java).apply {
                    putExtra(EXTRA_AID, aid)
                } to "视频 $aid"
            }
        }

        @IntentFilter
        class VideoIntentHandler2 : UrlOpenActivity.IIntentFilter {
            override fun handle(
                parsedIntent: UrlOpenActivity.ParsedIntent,
                context: Context
            ): Pair<Intent?, String?> {
                if (parsedIntent.host != "www.bilibili.com" || parsedIntent.paths.getOrNull(0) != "video") return null to null
                val p1 = parsedIntent.paths.getOrElse(1) { "0" }
                val aid = try {
                    p1.substring(2).toLong()
                } catch (e: Exception) {
                    p1.toAid()
                }
                return Intent(context, OnlinePlayActivity::class.java).apply {
                    putExtra(EXTRA_AID, aid)
                } to "视频 $aid"
            }
        }
    }

    class Model : ViewModel() {
        val relates = MutableLiveData<List<Relate>>(emptyList())
        val biliView = MutableLiveData<BiliView?>(null)
    }

    val aid by lazy { startIntent.getLongExtra(EXTRA_AID, 0) }
    var biliView
        get() = model.biliView.value
        set(value) {
            model.biliView.value = value
        }
    var cid = 0L
    var page = 1
        set(value) {
            if (field != value) {
                field = value
                onSetPage()
            }
        }
    val pageParserMap = mutableMapOf<Int, DanmakuParser>()

    var relateFragment: RelateFragment? = null
    val layoutOnlinePlayIntroBinding by lazy {
        LayoutOnlineplayIntroBinding.inflate(layoutInflater)
    }
    lateinit var viewPager2: ViewPager2
    val model by viewModels<Model>()

    // 保存长按加速前的播放速度
    var commonPlayRate: Float = 1.0f

    private val vibrator by lazy {
        VibratorUtil(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        System.out.println("---- onCreate OnlinePlayActivity")

        windowInsetsControllerCompat.isAppearanceLightStatusBars = false
    }

    override fun initViews() {
        super.initViews()
        supportActionBar?.title = "av$aid"

        when (baseBinding.rhv.tag) {
            "1" -> {
                val tabLayoutId = View.generateViewId()
                val tabLayout = TabLayout(this).apply {
                    id = tabLayoutId
                }
                if (layoutOnlinePlayIntroBinding.root.layoutParams != null)
                    layoutOnlinePlayIntroBinding.root.updateLayoutParams {
                        width = MATCH_PARENT
                        height = WRAP_CONTENT
                    }
                viewPager2 = ViewPager2(this).apply {
                    id = View.generateViewId()
                    adapter = PagerAdapter(this@OnlinePlayActivity)
                }
                baseBinding.rl.addView(tabLayout,
                    ConstraintLayout.LayoutParams(MATCH_CONSTRAINT, WRAP_CONTENT).apply {
                        topToBottom = R.id.rhv
                        startToStart = R.id.rhv
                        endToEnd = R.id.rhv
                    })
                baseBinding.rl.addView(viewPager2,
                    ConstraintLayout.LayoutParams(MATCH_CONSTRAINT, MATCH_CONSTRAINT).apply {
                        topToBottom = tabLayoutId
                        startToStart = R.id.rhv
                        endToEnd = R.id.rhv
                        bottomToBottom = PARENT_ID
                    })
                TabLayoutMediator(tabLayout, viewPager2) { tab, p ->
                    tab.text = when (p) {
                        0 -> "简介"
                        else -> "评论"
                    }
                }.attach()
            }
            "2" -> {
                baseBinding.rl.addView(layoutOnlinePlayIntroBinding.root.also {
                    it.id = View.generateViewId()
                },
                    ConstraintLayout.LayoutParams(MATCH_CONSTRAINT, MATCH_CONSTRAINT).apply {
                        topToBottom = R.id.rhv
                        startToStart = R.id.rhv
                        endToEnd = R.id.rhv
                        bottomToBottom = PARENT_ID
                    })
                val tabLayoutId = View.generateViewId()
                val tabLayout = TabLayout(this).apply {
                    id = tabLayoutId
                    ViewCompat.setOnApplyWindowInsetsListener(this) { v, vi ->
                        vi.maxSystemBarsDisplayCutout.let {
                            v.updatePadding(top = it.top, right = it.right)
                        }
                        vi
                    }
                }
                viewPager2 = ViewPager2(this).apply {
                    id = View.generateViewId()
                    adapter = PagerAdapter(this@OnlinePlayActivity)
                }
                baseBinding.rl.addView(tabLayout,
                    ConstraintLayout.LayoutParams(MATCH_CONSTRAINT, WRAP_CONTENT).apply {
                        startToEnd = R.id.rhv
                        endToEnd = PARENT_ID
                    })
                baseBinding.rl.addView(viewPager2,
                    ConstraintLayout.LayoutParams(MATCH_CONSTRAINT, MATCH_CONSTRAINT).apply {
                        topToBottom = tabLayoutId
                        startToStart = tabLayoutId
                        endToEnd = tabLayoutId
                        bottomToBottom = PARENT_ID
                    })
                TabLayoutMediator(tabLayout, viewPager2) { tab, p ->
                    tab.text = when (p) {
                        0 -> "相关"
                        else -> "评论"
                    }
                }.attach()
            }
        }
    }

    override fun initData() {
        super.initData()
        if (biliView == null) {
            runIOCatchingResultRunMain(this, {
                bilibiliClient.appAPI.view(aid = aid).await()
            }) { biliView ->
                biliView.data.redirectUrl.takeUnless { it.isNullOrBlank() }?.let {
                    BrowserUtil.openInApp(this, it)
                    finish()
                    return@runIOCatchingResultRunMain
                }

                this.biliView = biliView
                setCoverUrl(biliView.data.pic)
                supportActionBar?.title = biliView.data.title

                layoutOnlinePlayIntroBinding.biliView = biliView
                biliView.data.tag.forEach { tag ->
                    layoutOnlinePlayIntroBinding.cgTags.addView(
                        Chip(
                            this,
                            null,
                            R.attr.filterChip
                        ).apply {
                            text = tag.tagName
                            isCheckable = false
                            elevation = 0f
                            chipBackgroundColor = getColorStateList(R.color.quaternaryTextColor)
                            setOnClickListener {
                                BrowserUtil.openInApp(
                                    this@OnlinePlayActivity,
                                    "https://www.bilibili.com/v/channel/${tag.tagId}"
                                )
                            }
                        }, WRAP_CONTENT, WRAP_CONTENT
                    )
                }
                biliView.data.pages.forEach { p ->
                    layoutOnlinePlayIntroBinding.rgPages.addView(RadioButton(this).apply {
                        text = p.part
                        id = p.page
                        buttonDrawable = null
                        setBackgroundResource(R.drawable.rb_video_page_bg)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            setTextColor(
                                resources.getColorStateList(
                                    R.color.rb_video_page_text,
                                    theme
                                )
                            )
                        }
                        setPadding(10.dpToPx())
                    }, ViewGroup.MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        rightMargin = 5.dpToPx()
                    })
                    if (p.page == page)
                        layoutOnlinePlayIntroBinding.rgPages.check(p.page)
                }
                layoutOnlinePlayIntroBinding.rgPages.setOnCheckedChangeListener { _, page ->
                    this.page = page
                }
                layoutOnlinePlayIntroBinding.tvLike.apply {
                    text = biliView.data.stat.like.toString()
                    setOnClickListener {
                        runIOCatchingResultRunMain(this@OnlinePlayActivity,
                            { bilibiliClient.appAPI.like(aid = aid, like = 0).await() }) {
                            TipUtil.showTip(this@OnlinePlayActivity, it.data.toast)
                        }
                    }
                }
                layoutOnlinePlayIntroBinding.tvDislike.setOnClickListener {
                    runIOCatchingResultRunMain(this@OnlinePlayActivity,
                        { bilibiliClient.appAPI.dislike(aid = aid, dislike = 0).await() }) {}
                }
                layoutOnlinePlayIntroBinding.rvUp.setOnClickListener {
                    BrowserUtil.openInApp(this, "bilibili://space/${biliView.data.owner.mid}")
                }
                model.relates.value = Relate.parse(biliView.data.relates ?: emptyList())

                updateVideoPlayUrl()

                addGesture(biliPlayerView.playerControlView)  // 播放器手势
            }
        } else
            supportActionBar?.title = biliView!!.data.title
    }

    override fun onGetShare() = biliView?.data?.title to "https://bilibili.com/video/av$aid"

    override fun beforeReinitLayout() {
        super.beforeReinitLayout()
        layoutOnlinePlayIntroBinding.root.removeFromParent()
    }

    override fun onNextClick() {
        if (page <= biliView!!.data.pages.size) {
            page++
            updateVideoPlayUrl()
        }
    }

    private fun updateVideoPlayUrl() {
        cid = biliView!!.data.pages[page - 1].cid
        runIOCatchingResultRunMain(this,
            { bilibiliClient.playerAPI.videoPlayUrl(cid = cid, aid = aid).await() })
        {
            setVideoPlayUrl(it)
            prepare()
        }
    }

    private fun setVideoPlayUrl(videoPlayUrl: VideoPlayUrl) {
        if (videoPlayUrl.data.dash == null) {
            TipUtil.showTip(this, "不支持的形式")
            return
        }
        val title = biliView!!.data.title
        val pageTitle = biliView!!.data.pages[page - 1].part
        val hasAudio = videoPlayUrl.data.dash!!.audio != null
        val sources = mutableListOf<PlayInfo.Source>()
        videoPlayUrl.data.dash!!.video.forEach { video ->
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(video.baseUrl))
            val audioSource =
                if (hasAudio) ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(videoPlayUrl.data.dash!!.audio!![0].baseUrl))
                else null
            val mergedSource =
                if (hasAudio) MergingMediaSource(videoSource, audioSource!!) else videoSource
            val backups = mutableListOf<MediaSource>()
            video.backupUrl?.forEach { backup ->
                val bv = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(backup))
                val mb = if (hasAudio) MergingMediaSource(bv, audioSource!!) else bv
                backups.add(mb)
            }
            val name = videoPlayUrl.data.acceptDescription.getOrNull(
                videoPlayUrl.data.acceptQuality.indexOf(video.id)
            ) ?: video.id.toString()
            sources.add(PlayInfo.Source(name, video.id, mergedSource, backups))
        }
        if (hasAudio)
            sources.add(
                PlayInfo.Source(
                    "audio only", 0,
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(videoPlayUrl.data.dash!!.audio!![0].baseUrl)),
                    emptyList()
                )
            )

        val danmakuParser = pageParserMap[page]
            ?: LazyCidDanmakuParser(aid, cid, biliView!!.data.pages[page - 1].duration).also {
                pageParserMap[page] = it
            }
        val onlinePlayQuality = Settings.onlinePlayQuality
        biliPlayerView.playInfo = PlayInfo(
            title,
            pageTitle,
            sources,
            danmakuParser,
            biliView!!.data.pages.size > page,
            sources.find { it.id == onlinePlayQuality } ?: sources.first()
        )
    }

    private fun onSetPage() {
        updateVideoPlayUrl()
        layoutOnlinePlayIntroBinding.rgPages.check(page)
    }

    inner class PagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            return if (position == 0)
                RelateFragment().apply {
                    if (this@OnlinePlayActivity.baseBinding.rhv.tag == "1")
                        header = layoutOnlinePlayIntroBinding.root
                }.also {
                    relateFragment = it
                }
            else
                RootCommentFragment(aid, 1)
        }
    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        insets.maxSystemBarsDisplayCutout.let {
            layoutOnlinePlayIntroBinding.llRoot.updatePadding(bottom = if (baseBinding.rhv.tag == "2") it.bottom else 0)
        }
        with(insets.maxSystemBarsDisplayCutoutIme) {
            viewPager2.updatePadding(
                right = if (baseBinding.rhv.tag == "2") right else 0,
                bottom = bottom
            )
        }
    }

    override fun onFirstPlay() {
        addHistory()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (played)
            addHistory()
    }

    override fun getVideoRatioin(): Rational {
        if (biliView == null)
            return super.getVideoRatioin()
        val page = biliView!!.data.pages[page - 1]
        val width = page.dimension.width
        val height = page.dimension.height
        return Rational(width, height)
    }

    private fun addHistory() {
        val time = biliPlayerView.player.contentPosition / 1000
        runIOCatchingResultRunMain(this,
            { bilibiliClient.webAPI.heartbeat(aid, cid = cid, playedTime = time).await() }) {}
    }

    /**
     * @desp playerView 的手势探测器
     */
    private lateinit var mPlayerViewGesture: GestureDetectorCompat

    /**
     * @desp playerView 的触摸监听
     */
    private lateinit var playerViewOnTouchListener: OnTouchListener

    private fun addGesture(view: View) {

        mPlayerViewGesture = GestureDetectorCompat(this, playerViewGestureListener)
//        playerViewOnTouchListener = object : OnTouchListener {}

        playerViewOnTouchListener = OnTouchListener { v, event ->
            view.onTouchEvent(event!!)
            mPlayerViewGesture.onTouchEvent(event)

            if (event.action == MotionEvent.ACTION_UP) {
                // TODO: cancel long press
                Log.e("onLongPress", "onLongPress ACTION_UP")
                if (biliPlayerView.player.playbackParameters.speed != this.commonPlayRate) {
                    val newParams =
                        biliPlayerView.player.playbackParameters.withSpeed(this.commonPlayRate)
                    biliPlayerView.player.setPlaybackParameters(newParams)

                    vibrator.vibrate(VibrationEffect.EFFECT_CLICK)
                }

                v!!.performClick()
            } else {
                true
            }
        }

        view.setOnTouchListener(playerViewOnTouchListener)
//        baseBinding.rhv.setOnTouchListener(playerViewOnTouchListener)
    }

    enum class SwipeDirection {
        UP, DOWN, LEFT, RIGHT
    }

    /**
     * @desp playerView 的手势监听
     */
    private val playerViewGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        //        private val FLING_MIN_DISTANCE = 20 // 移动最小距离
        private val FLING_MIN_VELOCITY = 200 // 移动最小速度
        private val DEBUG_TAG = "Gestures"


        fun onSwipe(direction: SwipeDirection) {
            Log.d(DEBUG_TAG, "onFling: " + direction.name)
            val periodSec = 5  // 跳转5秒

            when (direction) {
                SwipeDirection.LEFT -> biliPlayerView.player.let {
                    val back = it.contentPosition - periodSec * 1000
                    it.seekTo(max(0, back))
                }
                SwipeDirection.RIGHT -> biliPlayerView.player.let {
                    val forward = it.contentPosition + periodSec * 1000
                    it.seekTo(min(forward, it.duration))
                }
                SwipeDirection.UP -> biliPlayerView.let {
                    if (isFullScreen.not()) it.changeFullscreen()
                }
                SwipeDirection.DOWN -> biliPlayerView.let {
                    if (isFullScreen)
                        biliPlayerView.changeFullscreen()
                    else {
                        enterPiP()
                    }
                }
            }
        }

        override fun onLongPress(e: MotionEvent?) {
//            e.action == MotionEvent.
            Log.e("onLongPress", "onLongPress " + e?.action.toString())

            if (biliPlayerView.player.isPlaying) {
                if (biliPlayerView.playerView.isControllerFullyVisible) biliPlayerView.playerControlView.hide()

                this@OnlinePlayActivity.commonPlayRate =
                    biliPlayerView.player.playbackParameters.speed
                val newParams = biliPlayerView.player.playbackParameters.withSpeed(2.0f)
                biliPlayerView.player.setPlaybackParameters(newParams)

                vibrator.vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)
            }

            super.onLongPress(e)
        }

        override fun onShowPress(e: MotionEvent?) {
            Log.e("onShowPress", "onShowPress " + e?.action.toString())
            super.onShowPress(e)
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if (biliPlayerView.playerView.isControllerFullyVisible)
                biliPlayerView.playerView.hideController()
            else
                biliPlayerView.playerView.showController()
            return super.onSingleTapUp(e)
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            Log.e("onDoubleTap", "biliPlayerView.isPlaying " + biliPlayerView.player.isPlaying)
            if (biliPlayerView.player.isPlaying) {
                biliPlayerView.pause()
            } else {
                biliPlayerView.resume()
            }

            return super.onDoubleTap(e)
        }

        override fun onDown(event: MotionEvent): Boolean {
            Log.d(DEBUG_TAG, "onDown: $event")
            return true
        }

        override fun onFling(
            e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            val translateX = e2.x - e1.x  // 水平移动距离
            val translateY = e2.y - e1.y  // 垂直移动距离
            // 垂直
            if (abs(velocityY) > FLING_MIN_VELOCITY && abs(translateY) > abs(translateX)) {
                // 向上滑动
                if (translateY < 0) {
                    onSwipe(SwipeDirection.UP)
                    return false
                }
                // 向下滑动
                if (translateY > 0) {
                    onSwipe(SwipeDirection.DOWN)
                    return false
                }
            }

            // 水平
            if (abs(velocityX) > FLING_MIN_VELOCITY && abs(translateX) > abs(translateY)) {
                // 向左滑动
                if (translateX < 0) {
                    onSwipe(SwipeDirection.LEFT)
                    return false
                }
                // 向右滑动
                if (translateX > 0) {
                    onSwipe(SwipeDirection.RIGHT)
                    return false
                }
            }
            return false
        }
    }

}