package com.bhm.flowhttp.core

import com.bhm.flowhttp.define.OK_CODE
import com.google.gson.Gson
import retrofit2.Converter
import retrofit2.Retrofit
import okhttp3.ResponseBody
import okhttp3.RequestBody
import java.lang.NullPointerException
import java.lang.reflect.Type

class ResponseConverterFactory private constructor(
    gson: Gson?,
    private val messageKey: String = "message",
    private val codeKey: String = "code",
    private val dataKey: String = "data",
    private val successCode: Int = OK_CODE
) : Converter.Factory() {
    private val gson: Gson
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        //返回我们自定义的Gson响应体变换器
        return GsonResponseBodyConverter<Any>(gson, type, messageKey, codeKey, dataKey, successCode)
    }

    override fun requestBodyConverter(
        type: Type, parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>, retrofit: Retrofit
    ): Converter<*, RequestBody> {
        //返回我们自定义的Gson响应体变换器
        return GsonResponseBodyConverter(gson, type, messageKey, codeKey, dataKey, successCode)
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun create(gson: Gson? = Gson(),
                   messageKey: String,
                   codeKey: String,
                   dataKey: String,
                   successCode: Int = OK_CODE): ResponseConverterFactory {
            return ResponseConverterFactory(gson, messageKey, codeKey, dataKey, successCode)
        }
    }

    init {
        if (gson == null) throw NullPointerException("gson == null")
        this.gson = gson
    }
}