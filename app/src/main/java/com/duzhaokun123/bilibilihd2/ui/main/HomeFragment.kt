package com.duzhaokun123.bilibilihd2.ui.main

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.duzhaokun123.bilibilihd2.BR
import com.duzhaokun123.bilibilihd2.R
import com.duzhaokun123.bilibilihd2.bases.BaseSimpleCardGridSRRVFragment
import com.duzhaokun123.bilibilihd2.databinding.ItemHomeCardBinding
import com.duzhaokun123.bilibilihd2.model.HomeCardModel
import com.duzhaokun123.bilibilihd2.ui.search.SearchActivity
import com.duzhaokun123.bilibilihd2.utils.*
import com.duzhaokun123.generated.Settings

class HomeFragment :
    BaseSimpleCardGridSRRVFragment<ItemHomeCardBinding, HomeCardModel, HomeFragment.HomeModel>(
        R.layout.item_home_card,
        Settings.mainCardWidthDp.let { run { if (it == 0) 500 else it }.dpToPx() },
        HomeModel::class
    ) {
    class HomeModel : BaseModel<HomeCardModel>()

    override suspend fun onRefreshIO(): List<HomeCardModel>? {
        return runCatching {
            HomeCardModel.parse(bilibiliClient.appAPI.homePage(pull = true).await())
        }.commonOnFailureHandler(context).getOrNull()
    }

    override suspend fun onLoadMorIO(): List<HomeCardModel>? {
        return runCatching {
            HomeCardModel.parse(bilibiliClient.appAPI.homePage(pull = false).await())
        }.commonOnFailureHandler(context).getOrNull()
    }

    override fun initItemView(
        itemBinding: ItemHomeCardBinding, itemModel: HomeCardModel, position: Int
    ) {
        itemBinding.cv.setOnClickListener {
            itemModel.uri?.let { BrowserUtil.openInApp(context, it) }
        }
        itemBinding.cv.setOnLongClickListener {
            itemBinding.ibTp.callOnClick()
            true
        }
        itemBinding.civFace.setOnClickListener {
            itemModel.upUri?.let { BrowserUtil.openInApp(context, it) }
        }
        itemBinding.ibTp.setOnClickListener {
            PopupMenu(requireContext(), itemBinding.ibTp).apply {
                menu.add("检查封面").setOnMenuItemClickListener {
                    ImageViewUtil.viewImage(requireActivity(), itemModel.coverUrl, itemBinding.iv)
                    true
                }
            }.show()
        }
    }

    override fun initItemData(
        itemBinding: ItemHomeCardBinding, itemModel: HomeCardModel, position: Int
    ) {
//        itemBinding.model = itemModel
        itemBinding.setVariable(BR.model, itemModel)
        itemBinding.executePendingBindings()
    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        with(insets.maxSystemBarsDisplayCutout) {
            baseBinding.srl.updatePadding(left = left, right = right, bottom = bottom)
//            baseBinding.cf.updateLayoutParams<ViewGroup.MarginLayoutParams> {
//                bottomMargin = bottom
//            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_search) {
            requireContext().startActivity<SearchActivity>()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        if (baseBinding.rv.canScrollVertically(-1)) {
            baseBinding.rv.smoothScrollToPosition(0)
        } else {
            // 已到顶部
            baseBinding.srl.autoRefresh()
        }
    }
}