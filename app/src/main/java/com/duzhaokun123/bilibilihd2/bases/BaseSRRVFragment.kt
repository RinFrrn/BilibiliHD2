package com.duzhaokun123.bilibilihd2.bases

import android.annotation.SuppressLint
import android.content.Context
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import com.duzhaokun123.bilibilihd2.R
import com.duzhaokun123.bilibilihd2.databinding.FragmentBaseSrrvBinding
import com.duzhaokun123.bilibilihd2.utils.CustomCubicBezier
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.layout.api.RefreshFooter
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.SpinnerStyle
import com.scwang.smart.refresh.layout.listener.DefaultRefreshFooterCreator

/**
 * SRRV: SmartRefresh RecycleView
 */
abstract class BaseSRRVFragment :
    io.github.duzhaokun123.androidapptemplate.bases.BaseFragment<FragmentBaseSrrvBinding>(R.layout.fragment_base_srrv) {

    var adapter
        get() = baseBinding.rv.adapter
        set(value) {
            baseBinding.rv.adapter = value
        }

    //    val srl: SmartRefreshBridge by lazy {
//        SmartRefreshBridge(baseBinding.srl)
//    }
    val srl get() = baseBinding.srl

    @CallSuper
    override fun initViews() {
        // 尝试解决滑动卡顿问题，即使设置较大值同样存在不复用的问题，不能根治
//        baseBinding.rv.setItemViewCacheSize(36)

//        baseBinding.srl.setDelegate(this)
//        val refreshHolder = BGANormalRefreshViewHolder(context, true)
//        baseBinding.srl.setRefreshViewHolder(refreshHolder)

        baseBinding.srl.setOnLoadMoreListener(::onLoadMore)
        baseBinding.srl.setOnRefreshListener(::onRefresh)

        setupRefreshLayout()

        baseBinding.rv.layoutManager = initLayoutManager()
    }

    abstract fun onRefresh(refreshLayout: RefreshLayout)
    abstract fun onLoadMore(refreshLayout: RefreshLayout)
    abstract fun initLayoutManager(): RecyclerView.LayoutManager


    private fun setupRefreshLayout() {
        val refreshLayout = baseBinding.srl
//        refreshLayout.setHeaderTriggerRate(0.64f)

        refreshLayout.setReboundDuration(250)
        refreshLayout.setReboundInterpolator(
            CustomCubicBezier.smoothSheetInterpolator()
        )

        refreshLayout.setEnableNestedScroll(false)  // 是否启用嵌套滚动

//        refreshLayout.setEnablePureScrollMode(true)  // 是否启用纯滚动模式
        refreshLayout.setEnableOverScrollDrag(true)  // 是否启用越界拖动（仿苹果效果）1.0.4
        refreshLayout.setEnableOverScrollBounce(true)  // 是否启用越界回弹

//        refreshLayout.setFooterTriggerRate(2.0f)
//        refreshLayout.setEnableFooterFollowWhenNoMoreData(false);//是否在全部加载结束之后Footer跟随内容1.0.4
        refreshLayout.setPrimaryColorsId(R.color.primaryColor)

        val footer = ClassicsFooter(context)
        footer.setFinishDuration(0)
        footer.spinnerStyle = SpinnerStyle.Translate
        refreshLayout.setRefreshFooter(footer)
    }
//    override fun onBGARefreshLayoutBeginRefreshing(refreshLayout: RefreshLayout?) {
//        refreshLayout?.let { onRefresh(it) }
//    }
//
//    override fun onBGARefreshLayoutBeginLoadingMore(refreshLayout: RefreshLayout?): Boolean {
//        refreshLayout?.let { onLoadMore(it) }
//        return true
//    }

}