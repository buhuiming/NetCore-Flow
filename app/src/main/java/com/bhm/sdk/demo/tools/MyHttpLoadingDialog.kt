package com.bhm.sdk.demo.tools

import com.bhm.network.core.HttpOptions
import com.bhm.network.base.HttpLoadingDialog
import com.bhm.network.base.HttpLoadingFragment

/**
 * Created by bhm on 2023/5/6.
 */
class MyHttpLoadingDialog : HttpLoadingDialog() {
    override fun initDialog(builder: HttpOptions): HttpLoadingFragment {
        return MyHttpLoadingFragment(builder)
    }
}