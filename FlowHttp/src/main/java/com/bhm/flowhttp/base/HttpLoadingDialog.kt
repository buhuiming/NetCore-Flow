package com.bhm.flowhttp.base

import com.bhm.flowhttp.core.HttpBuilder
import android.app.Activity

/**
 * Created by bhm on 2023/5/6.
 */
open class HttpLoadingDialog {

    private var httpLoadingFragment: HttpLoadingFragment? = null

    private var showAgain = false

    /**
     * rxManager 用户按返回关闭，请求取消
     * isCancelable true,单击返回键，dialog关闭；false,1s内双击返回键，dialog关闭，否则dialog不关闭
     */
    fun showLoading(builder: HttpBuilder) {
        if (!builder.activity.isFinishing && builder.isShowDialog) {
            if (httpLoadingFragment == null) {
                httpLoadingFragment = initDialog(builder)
            }
            val fm = builder.activity.supportFragmentManager
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

    open fun initDialog(builder: HttpBuilder): HttpLoadingFragment {
        return HttpLoadingFragment(builder)
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

    companion object {
        private var RxDialog: HttpLoadingDialog? = null
        val defaultDialog: HttpLoadingDialog?
            get() {
                if (null == RxDialog) {
                    RxDialog = HttpLoadingDialog()
                }
                return RxDialog
            }
    }
}