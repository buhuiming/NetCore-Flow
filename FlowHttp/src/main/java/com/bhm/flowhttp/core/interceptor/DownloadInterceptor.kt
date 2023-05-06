package com.bhm.flowhttp.core.interceptor

import com.bhm.flowhttp.body.DownloadResponseBody
import com.bhm.flowhttp.core.HttpBuilder
import okhttp3.Interceptor

/**
 * @author Buhuiming
 * @description: 下载进度拦截器
 * @date :2023/5/6
 */
class DownloadInterceptor {
    fun make(builder: HttpBuilder): Interceptor {
        return Interceptor { chain: Interceptor.Chain ->
            val response = chain.proceed(chain.request())
            response.body?.let {

            }
            response.newBuilder().body(
                response.body?.let { DownloadResponseBody(it, builder) }
            ).build()
        }
    }
}