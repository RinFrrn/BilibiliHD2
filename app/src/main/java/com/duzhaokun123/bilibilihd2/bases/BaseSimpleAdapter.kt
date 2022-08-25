package com.duzhaokun123.bilibilihd2.bases

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.duzhaokun123.bilibilihd2.BR

abstract class BaseSimpleAdapter<BaseBinding : ViewDataBinding>(
    val context: Context,
    @LayoutRes val layoutId: Int
) : RecyclerView.Adapter<BaseSimpleAdapter.BaseBindVH<BaseBinding>>() {

    class BaseBindVH<BaseBinding : ViewDataBinding>(val baseBinding: BaseBinding) :
        RecyclerView.ViewHolder(baseBinding.root)

    companion object {
        var count = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseBindVH<BaseBinding> {
        val baseBind = DataBindingUtil.inflate<BaseBinding>(
            LayoutInflater.from(context), layoutId, parent, false
        )
        val holder = BaseBindVH(baseBind)
//        System.out.println("---- onCreateViewHolder count: " + count++)
        return holder
    }

    override fun onBindViewHolder(holder: BaseBindVH<BaseBinding>, position: Int) {
        initViews(holder.baseBinding, position)
        initData(holder.baseBinding, position)
//        System.out.println("---- onBindViewHolder position: ${position}")
    }

    abstract fun initViews(baseBinding: BaseBinding, position: Int)
    abstract fun initData(baseBinding: BaseBinding, position: Int)
}