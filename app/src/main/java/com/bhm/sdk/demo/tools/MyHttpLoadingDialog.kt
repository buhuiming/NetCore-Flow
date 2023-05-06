package com.bhm.sdk.demo.tools

import com.bhm.flowhttp.core.HttpBuilder
import com.bhm.flowhttp.base.HttpLoadingDialog
import com.bhm.flowhttp.base.HttpLoadingFragment

/**
 * Created by bhm on 2023/5/6.
 */
class MyHttpLoadingDialog : HttpLoadingDialog() {
    override fun initDialog(builder: HttpBuilder?): HttpLoadingFragment {
        return MyHttpLoadingFragment(builder!!)
    }
}