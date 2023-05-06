package com.bhm.flowhttp.core

import com.bhm.flowhttp.adapter.IntegerDefaultAdapter
import com.bhm.flowhttp.adapter.LongDefaultAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Retrofit

/**
 * Created by bhm on 2023/5/6.
 */
class RetrofitHelper(private val builder: HttpBuilder) {

    fun <T> createRequest(clazz: Class<T>, url: String): T {
        if (builder.isShowDialog && null != builder.dialog) {
            builder.dialog?.showLoading(builder)
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(GenerateOkHttpClient().make(builder))
            .addConverterFactory(ResponseConverterFactory.create(
                gsonBuilder,
                builder.messageKey,
                builder.codeKey,
                builder.dataKey,
                builder.successCode)
            )
            .build()
        return retrofit.create(clazz)
    }

    private val gsonBuilder: Gson
        get() = GsonBuilder()
            .registerTypeAdapter(Int::class.java, IntegerDefaultAdapter())
            .registerTypeAdapter(Int::class.javaPrimitiveType, IntegerDefaultAdapter())
            .registerTypeAdapter(Long::class.java, LongDefaultAdapter())
            .registerTypeAdapter(Long::class.javaPrimitiveType, LongDefaultAdapter())
            .create()
}