package com.duzhaokun123.bilibilihd2.ui.play.online

import android.content.Context
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.ColorUtils
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.duzhaokun123.bilibilihd2.R
import com.duzhaokun123.bilibilihd2.bases.BaseSimpleWithHeaderAdapter
import com.duzhaokun123.bilibilihd2.databinding.ItemRelateCardBinding
import com.duzhaokun123.bilibilihd2.databinding.LayoutRecycleViewBinding
import com.duzhaokun123.bilibilihd2.utils.*
import io.github.duzhaokun123.androidapptemplate.bases.BaseFragment
import io.material.catalog.tableofcontents.GridDividerDecoration
import kotlin.math.ceil
import com.hiczp.bilibili.api.app.model.View as BiliView

class RelateFragment : BaseFragment<LayoutRecycleViewBinding>(R.layout.layout_recycle_view) {
    lateinit var relates: List<Relate>
    var header: View? = null
    val model by activityViewModels<OnlinePlayActivity.Model>()

    override fun initViews() {
//        baseBinding.rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val gridLayoutManager = GridLayoutManager(context, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val lm = baseBinding.rv.layoutManager as GridLayoutManager
                return if (position == 0 && this@RelateFragment.header != null) lm.spanCount else 1
            }
        }
        baseBinding.rv.layoutManager = gridLayoutManager
        baseBinding.rv.addOnLayoutChangeListener { rv, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->

            val H = bottom - top
            val oldH = oldBottom - oldTop
            val W = right - left
            val oldW = oldRight - oldLeft

            if (H != oldH && W != oldW) {
                runMain {
                    // change span count...
                    val recyclerView = this@RelateFragment.baseBinding.rv
                    val lm = recyclerView.layoutManager as GridLayoutManager
                    lm.spanCount = getColumnCount(recyclerView)

                    // change horizontal padding
                    println("---- recyclerView.width OnLayoutChange ${recyclerView.width}")
                    val horiPadding = if (recyclerView.width in 800..999) 6.dpToPx() else 10.dpToPx()
                    recyclerView.updatePadding(left = horiPadding, right = horiPadding)

//                    baseBinding.sv
                }
//                println("---- RelateFragment OnLayoutChange ${lm.spanCount}")
            }
        }

        baseBinding.rv.adapter = Adapter(requireContext()).apply {
            headerView = header ?: headerView
        }
//        runCatching { baseBinding.rv.removeItemDecorationAt(0) }
//        baseBinding.rv.addItemDecoration(
//            GridDividerDecoration(
//                1.dpToPx(),
//                ColorUtils.setAlphaComponent(
//                    requireContext().theme.getAttr(R.attr.colorOnSurface).data,
//                    (255 * 0.12).toInt()
//                ),
//                1
//            ), 0
//        )
    }

    override fun initData() {
        model.relates.observe(this) { relates ->
            this.relates = relates
            baseBinding.rv.resetAdapter()
        }
    }

    /**
     * Returns grid spanCount
     * Called on onLayoutChangeListener
     * Default is reactiveColumn
     */
    private fun getColumnCount(view: View): Int {
        return reactiveColumn(view)
    }

    private fun reactiveColumn(view: View): Int {
        // set card max width with 360dp
        val cardMaxWidth = 300f.dpToPx()
        return ceil(view.width.toDouble() / cardMaxWidth).toInt()
    }

    inner class Adapter(context: Context) : BaseSimpleWithHeaderAdapter<ItemRelateCardBinding>(
        context, R.layout.item_relate_card
    ) {
        override val itemCountNoHeader: Int
            get() = relates.size

        override fun initView(baseBinding: ItemRelateCardBinding, position: Int) {

        }

        override fun initData(baseBinding: ItemRelateCardBinding, position: Int) {
            relates[position].let { relate ->
                baseBinding.relate = relate
                baseBinding.cv.setOnClickListener {
                    BrowserUtil.openInApp(context, relate.url)
                }
                baseBinding.cv.setOnLongClickListener {
                    baseBinding.ibTp.callOnClick()
                    true
                }
                baseBinding.ibTp.setOnClickListener {
                    PopupMenu(requireContext(), baseBinding.ibTp).apply {
                        menu.add("检查封面").setOnMenuItemClickListener {
                            ImageViewUtil.viewImage(
                                requireActivity(),
                                relate.cover,
                                baseBinding.ivCover
                            )
                            true
                        }
                    }.show()
                }
            }
        }
    }
}

data class Relate(
    val title: String?,
    val cover: String?,
    val duration: String,
    val l1: String, // up / ad
    val l2: String, // 播放
    val l3: String, // 弹幕
    val url: String
) {
    companion object {
        fun parse(biliRelates: Collection<BiliView.Data.Relates>): List<Relate> {
            val re = mutableListOf<Relate>()
            biliRelates.forEach { biliRelate ->
                val title = biliRelate.title
                val cover = biliRelate.pic
                val duration = biliRelate.duration.takeIf { it != 0 }
                    ?.let { DateFormat.getStringForTime(it * 1000L) } ?: ""
                val l1 = biliRelate.owner?.name.takeUnless { it.isNullOrEmpty() }?.let { "up:$it" }
                    ?: "ad"
                val l2 = biliRelate.stat.view.takeIf { it != 0 }?.toString() ?: ""
                val l3 = biliRelate.stat.danmaku.takeIf { it != 0 }?.toString() ?: ""
//                val l2 = biliRelate.stat.view.takeIf { it != 0 }
//                    ?.let { "play:$it danmaku:${biliRelate.stat.danmaku}" } ?: ""
                val url = biliRelate.uri
                re.add(Relate(title, cover, duration, l1, l2, l3, url))
            }
            return re
        }
    }
}
