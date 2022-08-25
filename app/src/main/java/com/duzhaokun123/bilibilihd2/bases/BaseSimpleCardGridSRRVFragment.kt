package com.duzhaokun123.bilibilihd2.bases

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.Px
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.duzhaokun123.bilibilihd2.BR
import com.duzhaokun123.bilibilihd2.utils.dpToPx
import com.duzhaokun123.bilibilihd2.utils.runIO
import com.duzhaokun123.bilibilihd2.utils.runMain
import com.scwang.smart.refresh.layout.api.RefreshLayout
import kotlin.math.ceil
import kotlin.reflect.KClass


abstract class BaseSimpleCardGridSRRVFragment<ItemBinding : ViewDataBinding, ItemModel, ModelClass : BaseSimpleCardGridSRRVFragment.BaseModel<ItemModel>>(
    @LayoutRes private val itemLayoutId: Int,
    @Px private val hopeCardWidth: Int,
    modelClass: KClass<ModelClass>,
    // null: default R.attr.colorOnSurface at 2%
    private val dividerColor: Int? = null
) : BaseSRRVFragment() {
    abstract class BaseModel<ItemModel> : ViewModel() {
        val itemModel = MutableLiveData<List<ItemModel>>(emptyList())
    }

    val baseModel by createViewModelLazy(modelClass, { requireActivity().viewModelStore },
        { requireActivity().defaultViewModelProviderFactory })

    var items
        get() = baseModel.itemModel.value!!
        set(value) {
            baseModel.itemModel.value = value
        }

    @CallSuper
    override fun onRefresh(refreshLayout: RefreshLayout) {
        runIO {
            val n = onRefreshIO()
            runMain {
                if (n == null) srl.finishRefresh(false)
                else {
                    items = n + items
                    srl.finishRefresh()
                    adapter!!.notifyDataSetChanged()
//                    adapter!!.notifyItemRangeInserted(0, n.size)
                }
            }
        }
    }

    @CallSuper
    override fun onLoadMore(refreshLayout: RefreshLayout) {
        runIO {
            val n = onLoadMorIO()
            runMain {
                if (n == null) srl.finishLoadMore(false)
                else {
                    val oldCount = items.size
                    items = items + n
                    srl.finishLoadMore()
                    adapter!!.notifyItemRangeInserted(oldCount, n.size)
                }
            }
        }
    }

    override fun initLayoutManager(): RecyclerView.LayoutManager {
//        System.out.println("---- initLayoutManager")
        return GridLayoutManager(context, 2)
    }

    /**
     * Returns grid spanCount
     * Called on onLayoutChangeListener
     * Default is reactiveColumn
     */
    open fun getColumnCount(view: View): Int {
        return reactiveColumn(view)
    }

    fun hopeCardWColumn(view: View): Int {
        val l = view.width / hopeCardWidth

        return if (l == 0) {
            view.updatePadding(left = 0, right = 0)
            1
        } else {
            val p = (view.width - hopeCardWidth * l) / 2
            if (l == 1 && 2 * p <= hopeCardWidth / 3) {
                view.updatePadding(left = 0, right = 0)
            } else {
                view.updatePadding(left = p, right = p)
            }
            l
        }
    }

    fun reactiveColumn(view: View): Int {
        view.updatePadding(left = 0, right = 0)

        // set card max width with 360dp
        val cardMaxWidth = 360f.dpToPx()
        return ceil(view.width.toDouble() / cardMaxWidth).toInt()
    }

    @CallSuper
    override fun initViews() {
        super.initViews()

//        baseBinding.rv.addOnLayoutChangeListener { rv, _, _, _, _, _, _, _, _ ->
        baseBinding.rv.addOnLayoutChangeListener { rv, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val H = bottom - top
            val oldH = oldBottom - oldTop
            val W = right - left
            val oldW = oldRight - oldLeft

            if (H != oldH && W != oldW) {
                runMain {
                    val recyclerView = this@BaseSimpleCardGridSRRVFragment.baseBinding.rv//(rv as RecyclerView)
                    val lm = recyclerView.layoutManager as GridLayoutManager
                    lm.spanCount = getColumnCount(recyclerView)
                    println("---- OnLayoutChange ${lm.spanCount}")
                }
            }

//            val l = v.width / hopeCardWidth
//            val lm = baseBinding.rv.layoutManager as GridLayoutManager
//            if (l == 0) {
//                lm.spanCount = 1
//                v.updatePadding(left = 0, right = 0)
//            } else {
//                lm.spanCount = l
//                val p = (v.width - hopeCardWidth * l) / 2
//                if (l == 1 && 2 * p <= hopeCardWidth / 3) {
//                    v.updatePadding(left = 0, right = 0)
//                } else {
//                    v.updatePadding(left = p, right = p)
//                }
//            }


//            runCatching { baseBinding.rv.removeItemDecorationAt(0) }
//            baseBinding.rv.addItemDecoration(
//                GridDividerDecoration(
//                    1.dpToPx(),
//                    dividerColor ?: ColorUtils.setAlphaComponent(
//                        requireContext().theme.getAttr(R.attr.colorOnSurface).data,
//                        (255 * 0.12).toInt()
//                    ),
//                    lm.spanCount
//                ), 0
//            )
        }
    }

    @CallSuper
    override fun initData() {
        adapter = Adapter()
        if (items.isEmpty()) srl.autoRefresh()
    }

    fun setNoMoreData(v: Boolean) {
        srl.setNoMoreData(v)
    }

    /* null 加载失败*/
    abstract suspend fun onRefreshIO(): List<ItemModel>?
    abstract suspend fun onLoadMorIO(): List<ItemModel>?
    abstract fun initItemView(itemBinding: ItemBinding, itemModel: ItemModel, position: Int)
    abstract fun initItemData(itemBinding: ItemBinding, itemModel: ItemModel, position: Int)

    inner class Adapter : BaseSimpleAdapter<ItemBinding>(requireContext(), itemLayoutId) {

        override fun initViews(baseBinding: ItemBinding, position: Int) {
            initItemView(baseBinding, items[position], position)
        }

        override fun initData(baseBinding: ItemBinding, position: Int) {
            initItemData(baseBinding, items[position], position)
        }

        override fun getItemCount() = items.size
    }
}