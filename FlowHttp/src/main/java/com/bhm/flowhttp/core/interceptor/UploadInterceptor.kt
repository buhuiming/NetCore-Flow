package com.bhm.flowhttp.core.interceptor

import com.bhm.flowhttp.core.HttpBuilder
import com.bhm.flowhttp.body.UploadRequestBody
import okhttp3.Interceptor

/**
 * @author Buhuiming
 * @description: 上传进度拦截器
 * @date :2023/5/6
 */
class UploadInterceptor {
    fun make(builder: HttpBuilder?): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            if (null == request.body) {
                return@Interceptor chain.proceed(request)
            }
            val build = request.newBuilder()
                .method(
                    request.method,
                    UploadRequestBody(request.body!!, builder)
                )
                .build()
            chain.proceed(build)
        }
    }
}