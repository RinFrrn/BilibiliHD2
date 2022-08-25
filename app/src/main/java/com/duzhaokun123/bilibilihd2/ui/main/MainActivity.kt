package com.duzhaokun123.bilibilihd2.ui.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.findFragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.bapis.bilibili.app.card.v1.Base
import com.duzhaokun123.annotationProcessor.IntentFilter
import com.duzhaokun123.bilibilihd2.R
import com.duzhaokun123.bilibilihd2.databinding.ActivityMainBinding
import com.duzhaokun123.bilibilihd2.databinding.ActivityPlayBaseBinding
import com.duzhaokun123.bilibilihd2.navigation.setupWithNavController
import com.duzhaokun123.bilibilihd2.ui.TestActivity
import com.duzhaokun123.bilibilihd2.ui.UrlOpenActivity
import com.duzhaokun123.bilibilihd2.ui.settings.SettingsActivity
import com.duzhaokun123.bilibilihd2.utils.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.hiczp.bilibili.api.app.model.MyInfo
import io.github.duzhaokun123.androidapptemplate.bases.BaseActivity
import io.github.duzhaokun123.androidapptemplate.bases.BaseFragment
import io.github.duzhaokun123.androidapptemplate.utils.TipUtil
import io.github.duzhaokun123.androidapptemplate.utils.launch
import io.github.duzhaokun123.androidapptemplate.utils.onFailure
import io.github.duzhaokun123.androidapptemplate.utils.onSuccess

class MainActivity : BaseActivity<ActivityMainBinding>(
    R.layout.activity_main,
    Config.LAYOUT_MATCH_HORI,
    Config.NO_BACK,
    Config.NO_TOOL_BAR
) {

    @IntentFilter
    class MainActivityIntentFilter : UrlOpenActivity.IIntentFilter {
        override fun handle(
            parsedIntent: UrlOpenActivity.ParsedIntent, context: Context
        ): Pair<Intent?, String?> {
            if (parsedIntent.scheme != "bilibili") return null to null
            if (parsedIntent.host in arrayOf("home", "root")) {
                return Intent(context, MainActivity::class.java) to "main"
            }
            return null to null
        }
    }

    private lateinit var navController: NavController

    override fun initViews() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fcv) as NavHostFragment
        navController = navHostFragment.navController
        baseBinding.nv?.setupWithNavController(navController)
        baseBinding.nrv?.setupWithNavController(navController)
        baseBinding.bnv?.setupWithNavController(navController)
        baseBinding.tb.setupWithNavController(
            navController,
            AppBarConfiguration.Builder(navController.graph).build()
        )
//        rootBinding.rootTb.setupWithNavController(
//            navController,
//            AppBarConfiguration.Builder(navController.graph).build()
//        )

        baseBinding.bnv?.setOnItemReselectedListener(::onNavigationItemReselected)
        baseBinding.nrv?.setOnItemReselectedListener(::onNavigationItemReselected)
//        baseBinding.nv?.setOnItemReselectedListener {
//            println("---- nv?.setOnItemReselectedListener")
//        }
    }

    fun onNavigationItemReselected(item: MenuItem) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fcv) as NavHostFragment
        val fragment = navHostFragment.childFragmentManager.fragments.first() as? BaseFragment<*>
        fragment?.onNavigationItemReselected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        beforeReinitLayout()
        reinitLayout()
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = navController.currentDestination?.label

    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        with(insets.maxSystemBarsDisplayCutout) {
//            rootBinding.rootAbl.updatePadding(left = left, right = right)
            baseBinding.abl.updatePadding(top = top, left = left, right = right)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val re = super.onCreateOptionsMenu(menu)
        menu ?: return re
        if (bilibiliClient.isLogin)
            menu.add(Menu.NONE, View.generateViewId(), 114514, "用户").apply {
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                io.github.duzhaokun123.androidapptemplate.utils.runIOCatching {
                    bilibiliClient.appAPI.myInfo().await()
                }.onFailure { t ->
                    t.printStackTrace()
                    TipUtil.showTip(this@MainActivity, t.localizedMessage)
                    setLoginMenuItem(this@apply)
                }.onSuccess { r ->
                    setUserMenuItem(this@apply, r.data)
                }.launch()
            }
        else
            setLoginMenuItem(menu.add(Menu.NONE, View.generateViewId(), 114514, "登录"))
        return true
    }

    private fun setUserMenuItem(menuItem: MenuItem, data: MyInfo.Data) {
        glideSafeGet(data.face) { b ->
            menuItem.icon = RoundedBitmapDrawableFactory.create(resources, b).apply {
                isCircular = true
                setAntiAlias(true)
            }
            menuItem.setOnMenuItemClickListener {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setIcon(RoundedBitmapDrawableFactory.create(resources, b).apply {
                        isCircular = true
                        setAntiAlias(true)
                    })
                    .setTitle(data.name)
                    .setMessage("UID: ${data.mid}\n硬币: ${data.coins}\n${data.sign}")
                    .setNegativeButton("设置") { _, _ -> startActivity<SettingsActivity>() }
//                                .setNegativeButtonIcon(ResourcesCompat.getDrawable(resources, R.drawable.ic_settings, theme))
                    .setPositiveButton("test") { _, _ -> startActivity<TestActivity>() }
                    .setNeutralButton("Space") { _, _ ->
                        BrowserUtil.openInApp(
                            this@MainActivity,
                            "bilibili://space/${data.mid}"
                        )
                    }
                    .show()
                return@setOnMenuItemClickListener true
            }
        }
    }

    fun setLoginMenuItem(menuItem: MenuItem) {
        runMain {
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menuItem.setOnMenuItemClickListener {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("未登录或无效")
                    .setMessage("前往 设置 -> 用户 -> 添加 以登录")
                    .setNegativeButton("设置") { _, _ -> startActivity<SettingsActivity>() }
//                                .setNegativeButtonIcon(ResourcesCompat.getDrawable(resources, R.drawable.ic_settings, theme))
                    .setPositiveButton("test") { _, _ -> startActivity<TestActivity>() }
                    .show()
                return@setOnMenuItemClickListener true
            }
        }
    }

    override fun initActionBar() = baseBinding.tb

    @CallSuper
    open fun beforeReinitLayout() {
        baseBinding.fcv.removeAllViews()
    }

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
            BaseActivity::class.java
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
    }
}