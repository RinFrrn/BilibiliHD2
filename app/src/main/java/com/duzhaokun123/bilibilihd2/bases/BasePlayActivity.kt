package com.duzhaokun123.bilibilihd2.bases

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.app.assist.AssistContent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Interpolator
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.*
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.databinding.DataBindingUtil
import androidx.transition.*
import com.bumptech.glide.Glide
import com.duzhaokun123.bilibilihd2.R
import com.duzhaokun123.bilibilihd2.Application
import com.duzhaokun123.bilibilihd2.databinding.ActivityPlayBaseBinding
import com.duzhaokun123.bilibilihd2.ui.settings.SettingsActivity
import com.duzhaokun123.bilibilihd2.utils.*
import com.duzhaokun123.biliplayer.BiliPlayerView
import com.duzhaokun123.generated.Settings
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.material.snackbar.Snackbar
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil


/**
 * 乱就乱 能用就行
 */
abstract class BasePlayActivity :
    io.github.duzhaokun123.androidapptemplate.bases.BaseActivity<ActivityPlayBaseBinding>(
        R.layout.activity_play_base,
        Config.NO_TOOL_BAR, Config.LAYOUT_MATCH_HORI
    ), StyledPlayerControlView.OnFullScreenModeChangedListener,
    StyledPlayerControlView.VisibilityListener {

    companion object {
        /** Intent action for media controls from Picture-in-Picture mode.  */
        private const val ACTION_MEDIA_CONTROL = "media_control"

        /** Intent extra for media controls from Picture-in-Picture mode.  */
        private const val EXTRA_CONTROL_TYPE = "control_type"

        /** The request code for play action PendingIntent.  */
        private const val REQUEST_PLAY = 1

        /** The request code for pause action PendingIntent.  */
        private const val REQUEST_PAUSE = 2

        /** The intent extra value for play action.  */
        private const val CONTROL_TYPE_PLAY = 1

        /** The intent extra value for pause action.  */
        private const val CONTROL_TYPE_PAUSE = 2

    }

    val dataSourceFactory by lazy {
        CacheDataSource.Factory().setCache(Application.simpleCache)
            .setUpstreamDataSourceFactory(DefaultDataSourceFactory(this))
    }

    var isFullScreen = false
        private set
    lateinit var biliPlayerView: BiliPlayerView
        private set
    val windowInsetsControllerCompat by lazy {
        WindowInsetsControllerCompat(window, rootBinding.root)
    }
    private var coverUrl: String? = null
    var played = false
    private var isPlayBeforeStop = false
    private val audioBecomingNoisyBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    private val mPictureInPictureParamsBuilder = PictureInPictureParams.Builder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(
            audioBecomingNoisyBroadcastReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )

        //添加Flag把状态栏设为可绘制模式
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        // 设置状态栏颜色
        windowInsetsControllerCompat.isAppearanceLightStatusBars = false
    }

    @CallSuper
    override fun findViews() {
        if (::biliPlayerView.isInitialized.not()) {
            biliPlayerView = BiliPlayerView(this).apply {
                id = R.id.bpv
                setPlayedColor(getColorCompat(R.color.biliPink))
                playerView.setControllerOnFullScreenModeChangedListener(this@BasePlayActivity)
                setBackgroundColor(getColorCompat(R.color.black))
                playerView.setControllerVisibilityListener(this@BasePlayActivity)
                onNextClickListener = this@BasePlayActivity::onNextClick
                ViewCompat.setElevation(this, 1.dpToPx().toFloat())
                player.addListener(object : Player.EventListener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            if (played.not()) {
                                played = true
                                onFirstPlay()
                            }
                        } else
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }

                    override fun onPlayerError(error: ExoPlaybackException) {
                        Snackbar.make(
                            rootBinding.rootCl,
                            error.cause?.localizedMessage ?: "null",
                            Snackbar.LENGTH_INDEFINITE
                        )
                            .setAction(R.string.retry) {
                                player.prepare()
                            }
                            .show()
                    }
                })
                danmakuView.zOnTop = Settings.danmakuOnTop
            }
        }
        baseBinding.rl.addView(
            biliPlayerView,
            1,
            ViewGroup.LayoutParams(MATCH_CONSTRAINT, MATCH_CONSTRAINT)
        )
        onFullScreenModeChanged(isFullScreen)
    }

    @CallSuper
    override fun initViews() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            baseBinding.abl.outlineProvider = null
        }
    }

    @CallSuper
    override fun initData() {

    }

    override fun initActionBar() = baseBinding.tb as Toolbar

    @CallSuper
    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        insets.maxSystemBarsDisplayCutout.let {
            when (baseBinding.rhv.tag) {
                "1" -> {
                    baseBinding.abl.updatePadding(top = it.top)
                    baseBinding.rhv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        leftMargin = it.left
                        rightMargin = it.right
                    }
                }
                "2" -> {
                    baseBinding.abl.updatePadding(top = it.top)
                    baseBinding.rhv.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        leftMargin = it.left
                    }
                }
            }
        }
        (if (isFullScreen) insets.displayCutoutInsets else Insets.NONE).let {
            biliPlayerView.updatePadding(
                left = it.left,
                top = it.top,
                right = it.right,
                bottom = it.bottom
            )
            baseBinding.abl.updatePadding(left = it.left, right = it.right)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        biliPlayerView.destroy()
        unregisterReceiver(audioBecomingNoisyBroadcastReceiver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // 画中画模式时不触发，否则进入画中画、改变画中画大小均会触发
        if (!isInPictureInPictureMode) {
            beforeReinitLayout()
            reinitLayout()
        }

        super.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        if (isFullScreen)
            biliPlayerView.changeFullscreen()
        else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.base_play, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_share -> {
                onGetShare().run {
                    if (first != null && second != null)
                        ShareUtil.shareUrl(this@BasePlayActivity, second!!, first)
                }
                true
            }
            R.id.item_retry -> {
                biliPlayerView.player.prepare()
                true
            }
            R.id.item_settings -> {
                startActivity<SettingsActivity>()
                true
            }
            R.id.item_pip -> {
                enterPiP()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        outContent.webUri = onGetShare().second?.toUri()
    }

    override fun onStart() {
        super.onStart()
        biliPlayerView.playerView.showController()
        if (Settings.playPauseTime == 1) {
            if (isPlayBeforeStop)
                resume()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Settings.playPauseTime == 1) {
            isPlayBeforeStop = biliPlayerView.player.playWhenReady
            pause()
        }
    }

    override fun onResume() {
        super.onResume()
        biliPlayerView.playerView.onResume()
        if (Settings.playPauseTime == 0) {
            if (isPlayBeforeStop)
                resume()
        }
        updateActions(R.drawable.ic_round_pause_24, "stop", null, REQUEST_PAUSE, CONTROL_TYPE_PAUSE)
    }

    override fun onPause() {
        super.onPause()

//        if (!isInPictureInPictureMode) {
        biliPlayerView.playerView.onPause()
        if (Settings.playPauseTime == 0) {
            isPlayBeforeStop = biliPlayerView.player.playWhenReady
            pause()
        }
//        }
    }

    fun start() {
        biliPlayerView.start()
    }

    fun stop() {
        biliPlayerView.stop()
    }

    fun pause() {
        biliPlayerView.pause()
    }

    fun resume() {
        biliPlayerView.resume()
    }

    fun prepare() {
        biliPlayerView.prepare()
    }

    fun setCoverUrl(url: String?) {
        coverUrl = url
        runNewThread {
            try {
                runMain {
                    biliPlayerView.setCover(url)
                }
//                val cover = Glide.with(this).load(url).submit().get()
//                runMain {
//                    biliPlayerView.setCover(cover)
//                }
            } catch (e: Exception) {
                e.printStackTrace()
                TipUtil.showTip(this, e.message)
            }
        }
    }

    @CallSuper
    override fun onFullScreenModeChanged(isFullScreen: Boolean) {
        this.isFullScreen = isFullScreen

        val useAnimation = !isInPictureInPictureMode

        if (useAnimation) {
            // add animation...
            val constraintLayout: ConstraintLayout = baseBinding.rl
            val transition = AutoTransition().apply {
                duration = 450
                interpolator = CustomCubicBezier(0.3f, 0.9f, 0.16f, 1.0f)
//            interpolator = CustomCubicBezier(0.36f, 0.44f, 0.06f, 1.0f)
            }
            TransitionManager.beginDelayedTransition(constraintLayout, transition)

            if (isFullScreen) {
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(
                    biliPlayerView.id,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                constraintSet.connect(
                    biliPlayerView.id,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                constraintSet.connect(
                    biliPlayerView.id,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                constraintSet.connect(
                    biliPlayerView.id,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
                constraintSet.applyTo(constraintLayout)

                windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.navigationBars())
                windowInsetsControllerCompat.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(
                    biliPlayerView.id,
                    ConstraintSet.TOP,
                    R.id.rhv,
                    ConstraintSet.TOP
                )
                constraintSet.connect(
                    biliPlayerView.id,
                    ConstraintSet.START,
                    R.id.rhv,
                    ConstraintSet.START
                )
                constraintSet.connect(
                    biliPlayerView.id,
                    ConstraintSet.BOTTOM,
                    R.id.rhv,
                    ConstraintSet.BOTTOM
                )
                constraintSet.connect(
                    biliPlayerView.id,
                    ConstraintSet.END,
                    R.id.rhv,
                    ConstraintSet.END
                )
                constraintSet.applyTo(constraintLayout)

                windowInsetsControllerCompat.show(WindowInsetsCompat.Type.navigationBars())
            }
        } else {

            if (isFullScreen) {
                biliPlayerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = PARENT_ID
                    startToStart = PARENT_ID
                    endToEnd = PARENT_ID
                    bottomToBottom = PARENT_ID
                }
                windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.navigationBars())
                windowInsetsControllerCompat.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                biliPlayerView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = R.id.rhv
                    startToStart = R.id.rhv
                    endToEnd = R.id.rhv
                    bottomToBottom = R.id.rhv
                }
                windowInsetsControllerCompat.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    override fun onVisibilityChange(visibility: Boolean) {
        baseBinding.abl.elevation = 2.dpToPx().toFloat()
        if (visibility) {
            supportActionBar?.show()
            windowInsetsControllerCompat.show(WindowInsetsCompat.Type.statusBars())
        } else {
            supportActionBar?.hide()
            windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.statusBars())
        }
        if (isFullScreen) {
            windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.navigationBars())
            windowInsetsControllerCompat.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if ((window.decorView.isVisible || biliPlayerView.isPlaying).not())
            finish()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        if (isInPictureInPictureMode) {
            if (isFullScreen.not()) biliPlayerView.changeFullscreen()
            biliPlayerView.playerView.useController = false
            registerReceiver(mReceiver, IntentFilter(ACTION_MEDIA_CONTROL))
        } else {
            if (isFullScreen) biliPlayerView.changeFullscreen()
            biliPlayerView.playerView.useController = true
            unregisterReceiver(mReceiver)
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    open fun onNextClick() {}

    /**
     * @return first: title second: url
     */
    open fun onGetShare(): Pair<String?, String?> = null to null

    @CallSuper
    open fun beforeReinitLayout() {
        baseBinding.rl.removeView(biliPlayerView)
    }

    open fun onFirstPlay() {
        System.out.println("---- onFirstPlay")
    }

    open fun getVideoRatioin() = Rational(16, 9)

    fun reinitLayout() {
        rootBinding.rootFl.removeAllViews()
        setAnyField(
            "baseBinding",
            DataBindingUtil.inflate<ActivityPlayBaseBinding>(
                layoutInflater,
                layoutId,
                rootBinding.rootFl,
                true
            ),
            io.github.duzhaokun123.androidapptemplate.bases.BaseActivity::class.java
        )
        findViews()
        setSupportActionBar(initActionBar())
        if (Config.NO_BACK !in configs) supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back)
        }
        initViews()
        initData()

        TipUtil.registerCoordinatorLayout(this, registerCoordinatorLayout())
//        onApplyWindowInsetsCompat(getAnyFieldAs<WindowInsetsCompatModel>("windowInsetsCompatModel").windowInsetsCompat.value!!)
    }

    open fun enterPiP() {

        runMain {
            supportActionBar?.hide()
            biliPlayerView.playerView.hideController()
        }

        runCatching {
            val aspectRatio = getVideoRatioin()
            var sourceRectHint = Rect()
//                Rect(
//                0,
//                0,
//                resources.displayMetrics.widthPixels,
//                resources.displayMetrics.heightPixels
//            )
            // FIXME: 竖屏视频过渡尺寸不一致
            biliPlayerView.playerView.getGlobalVisibleRect(sourceRectHint)
            System.out.println("---- enterPiP A $sourceRectHint, $aspectRatio")
            sourceRectHint.withAspectRatio(aspectRatio)
            System.out.println(
                "---- enterPiP B ${
                    arrayListOf<Int>(
                        sourceRectHint.left,
                        sourceRectHint.top,
                        sourceRectHint.right,
                        sourceRectHint.bottom
                    )
                }"
            )

            if (biliPlayerView.isPlaying) {
                updateActions(R.drawable.ic_round_pause_24, "stop", null, REQUEST_PAUSE, CONTROL_TYPE_PAUSE)
            } else {
                updateActions(R.drawable.ic_round_play_arrow_24, "play", null, REQUEST_PLAY, CONTROL_TYPE_PLAY)
            }

            mPictureInPictureParamsBuilder
                .setAspectRatio(aspectRatio)
                .setSourceRectHint(sourceRectHint)

            enterPictureInPictureMode(
                mPictureInPictureParamsBuilder.build()
            )
        }.onFailure {
            runCatching { enterPictureInPictureMode() }
        }.onFailure {
            TipUtil.showToast("系统不支持 PIP")
        }
    }

    /**
     * 更新画中画操作行为
     */
    fun updateActions(
        @DrawableRes iconId: Int, @NonNull title: String, @Nullable description: String?,
        @NonNull requestCode: Int, @NonNull controlCode: Int
    ) {
        val actions = arrayListOf<RemoteAction>()

        val intent = PendingIntent.getBroadcast(
            this,
            requestCode, Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlCode),
            PendingIntent.FLAG_IMMUTABLE
        )

        actions.add(
            RemoteAction(
                Icon.createWithResource(this, iconId),
                title,
                description ?: title,
                intent
            )
        )

        mPictureInPictureParamsBuilder.setActions(actions)
        setPictureInPictureParams(mPictureInPictureParamsBuilder.build())
    }

    /**
     * 接收播放行为
     */
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            intent?.let {
                if (intent.action != ACTION_MEDIA_CONTROL) {
                    return
                }

                val controlType = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)
                when (controlType) {
                    CONTROL_TYPE_PLAY -> {
                        biliPlayerView.resume()
                        //更新画中画的行为控件
                        updateActions(R.drawable.ic_round_pause_24, "stop", null, REQUEST_PAUSE, CONTROL_TYPE_PAUSE)
                    }
                    CONTROL_TYPE_PAUSE -> {
                        biliPlayerView.pause()
                        //更新画中画的行为控件
                        updateActions(R.drawable.ic_round_play_arrow_24, "play", null, REQUEST_PLAY, CONTROL_TYPE_PLAY)
                    }
                }
            }
        }
    }
}

fun Rect.withAspectRatio(aspectRatio: Rational): Rect {
    val rectRatio = Rational(this.width(), this.height())
    if (rectRatio > aspectRatio) {
        // 竖屏视频
        println("---- 竖屏视频 $rectRatio")
        val fittedWidth: Double =
            this.height()
                .toDouble() / aspectRatio.denominator * aspectRatio.numerator
        val horiInset = ((this.width() - fittedWidth) / 2).toInt()
        this.left += horiInset
        this.right -= horiInset
    } else if (rectRatio < aspectRatio) {
        // 横屏
        println("---- 横屏视频 $rectRatio")
        val fittedHeight: Double =
            this.width().toDouble() / aspectRatio.numerator * aspectRatio.denominator
        val vertInset = ((this.height() - fittedHeight) / 2).toInt()
        this.top += vertInset
        this.bottom -= vertInset
    }
    return this
}

fun Rect.fitAspectRatio(aspectRatio: Rational): Rect {
    val rectRatio = Rational(this.width(), this.height())
    return if (rectRatio == aspectRatio) {
        Rect(this.left, this.top, this.right, this.bottom)
    } else if (rectRatio > aspectRatio) {
        // 竖屏视频
        println("---- 竖屏视频 $rectRatio")
        val fittedWidth: Double =
            this.height()
                .toDouble() / aspectRatio.denominator.toDouble() * aspectRatio.numerator.toDouble()
        val horiInset = ((this.width() - fittedWidth) / 2).toInt()
        Rect(this.left + horiInset, this.top, this.right - horiInset, this.bottom)
    } else {
        // 横屏
        println("---- 横屏视频 $rectRatio")
        val fittedHeight: Double =
            this.width().toDouble() / aspectRatio.numerator * aspectRatio.denominator
        val vertInset = ((this.height() - fittedHeight) / 2).toInt()
        Rect(this.left, this.top + vertInset, this.right, this.bottom - vertInset)
    }
}
