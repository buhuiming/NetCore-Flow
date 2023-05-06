package com.bhm.flowhttp.base

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.bhm.flowhttp.R
import com.bhm.flowhttp.core.HttpBuilder
import java.util.*

open class HttpLoadingFragment(private val builder: HttpBuilder) : DialogFragment() {

    private var textView: TextView? = null

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            //在每个add事务前增加一个remove事务，防止连续的add
            manager.beginTransaction().remove(this).commitAllowingStateLoss()
            showAllowingLoss(manager, tag)
        } catch (e: Exception) {
            //同一实例使用不同的tag会异常,这里捕获一下
            e.printStackTrace()
        }
    }

    /**
     * 解决 Can not perform this action after onSaveInstanceState问题
     * @param manager FragmentManager
     * @param tag     tag
     */
    private fun showAllowingLoss(manager: FragmentManager, tag: String?) {
        try {
            val cls: Class<*> = DialogFragment::class.java
            val mDismissed = cls.getDeclaredField("mDismissed")
            mDismissed.isAccessible = true
            mDismissed[this] = false
            val mShownByMe = cls.getDeclaredField("mShownByMe")
            mShownByMe.isAccessible = true
            mShownByMe[this] = true
        } catch (e: Exception) {
            //调系统的show()方法
            show(manager, tag)
            return
        }
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

    override fun dismiss() {
        //防止横竖屏切换时 getFragmentManager置空引起的问题：
        //Attempt to invoke virtual method 'android.app.FragmentTransaction
        //android.app.FragmentManager.beginTransaction()' on a null object reference
        @Suppress("DEPRECATION")
        if (fragmentManager == null) return
        super.dismissAllowingStateLoss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = initDialog()
        if (activity != null) {
            dialog.setOwnerActivity(requireActivity())
            dialog.setCanceledOnTouchOutside(false) //这个值最好设置成false，点击其他区域关闭loading，体验效果不佳
            dialog.setCancelable(builder.isCancelable)
            dialog.setOnKeyListener(DialogInterface.OnKeyListener { _, i, keyEvent ->
                if (i == KeyEvent.KEYCODE_BACK && dialog.isShowing
                    && keyEvent.action == KeyEvent.ACTION_UP
                ) {
                    if (builder.isCancelable) {
                        if (builder.isDialogDismissInterruptRequest) {
                            builder.jobManager.removeJob()
                        }
                        dismiss()
                        return@OnKeyListener true
                    }
                    if (System.currentTimeMillis() - onBackPressed > 1000) {
                        onBackPressed = System.currentTimeMillis()
                    } else {
                        builder.jobManager.removeJob()
                        dismiss()
                    }
                }
                true
            })
        }
        return Objects.requireNonNull(dialog)
    }

    open fun initDialog(): Dialog {
        val dialog = Dialog(requireActivity(), R.style.loading_dialog) // 创建自定义样式dialog
        @SuppressLint("InflateParams")
        val view = activity?.layoutInflater?.inflate(R.layout.layout_dialog_app_loading, null) // 得到加载view
        view?.let {
            dialog.setContentView(
                it, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            ) // 设置布局
            textView = it.findViewById(R.id.dialog_text_loading)
        }
        if (!TextUtils.isEmpty(builder.loadingTitle)) {
            textView?.text = builder.loadingTitle
        }
        return dialog
    }

    /** 改变Dialog的显示内容
     * @param httpBuilder
     */
    fun changDialogContent(httpBuilder: HttpBuilder) {
        if (textView != null && !TextUtils.isEmpty(httpBuilder.loadingTitle)) {
            textView?.text = httpBuilder.loadingTitle
        }
    }

    companion object {
        private var onBackPressed = 0L
    }
}