package com.bhm.network.core.interceptor

import com.bhm.network.body.DownloadResponseBody
import com.bhm.network.core.HttpOptions
import okhttp3.Interceptor

/**
 * @author Buhuiming
 * @description: 下载进度拦截器
 * @date :2023/5/6
 */
class DownloadInterceptor {
    fun make(builder: HttpOptions): Interceptor {
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