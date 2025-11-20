package com.bhm.network.base

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.bhm.network.core.HttpOptions

/**
 * Created by bhm on 2023/5/6.
 */
open class HttpLoadingDialog {

    private var httpLoadingFragment: HttpLoadingFragment? = null

    private var showAgain = false

    companion object {
        const val KEY_DIALOG_CANCELABLE = "key_dialog_cancelable"

        const val KEY_DIALOG_DIALOG_DISMISS_INTERRUPT_REQUEST = "key_dialog_dialogDismissInterruptRequest"

        const val KEY_JOB_ID = "key_job_id"

        const val KEY_DIALOG_LOADING_TITLE = "key_dialog_loading_title"
    }

    /**
     * rxManager 用户按返回关闭，请求取消
     * isCancelable true,单击返回键，dialog关闭；false,1s内双击返回键，dialog关闭，否则dialog不关闭
     */
    fun showLoading(builder: HttpOptions) {
        if (builder.context is FragmentActivity && !(builder.context as FragmentActivity).isFinishing && builder.isShowDialog) {
            if (httpLoadingFragment == null) {
                httpLoadingFragment = initDialog(builder)
                httpLoadingFragment?.setCancelDialogEvent{
                    cancelLoading(builder.context as FragmentActivity)
                }
            }
            val fm = (builder.context as FragmentActivity).supportFragmentManager
            showAgain =
                if (httpLoadingFragment?.isAdded == false && null == fm.findFragmentByTag("default")) {
                    httpLoadingFragment?.show(fm, "default")
                    false
                } else {
                    httpLoadingFragment?.changDialogContent(builder)
                    true
                }
        }
    }

    fun close() {
        httpLoadingFragment?.dismiss()
        httpLoadingFragment = null
    }

    private fun initDialog(builder: HttpOptions): HttpLoadingFragment {
        val bundle = Bundle()
        bundle.putBoolean(KEY_DIALOG_CANCELABLE, builder.isCancelable)
        bundle.putBoolean(KEY_DIALOG_DIALOG_DISMISS_INTERRUPT_REQUEST, builder.isDialogDismissInterruptRequest)
        bundle.putString(KEY_JOB_ID, builder.jobKey)
        bundle.putString(KEY_DIALOG_LOADING_TITLE, builder.loadingTitle)
        return getLoadingFragment(bundle)
    }

    open fun getLoadingFragment(bundle: Bundle): HttpLoadingFragment {
        return HttpLoadingFragment().apply {
            arguments = bundle
        }
    }

    fun dismissLoading(activity: Activity?) {
        cancelLoading(activity)
    }

    private fun cancelLoading(activity: Activity?) {
        if (null != httpLoadingFragment && !showAgain && null != activity && null != httpLoadingFragment?.dialog && (activity
                    == httpLoadingFragment?.activity)
        ) {
            close()
        }
        showAgain = false
    }
}