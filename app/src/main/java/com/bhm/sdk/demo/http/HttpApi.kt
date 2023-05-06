package com.bhm.sdk.demo.http

import com.bhm.sdk.demo.entity.DoGetEntity
import com.bhm.sdk.demo.entity.DoPostEntity
import com.bhm.sdk.demo.entity.UpLoadEntity
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

/**
 * Created by bhm on 2023/5/6.
 */
interface HttpApi {
    @GET("/api/4/news/latest")
    suspend fun getData(
        @Header("token") token: String?,
        @Query("type") type: String?
    ): DoGetEntity

    @FormUrlEncoded
    @POST("apiv2/app/getCOSToken")
    suspend fun getDataPost(
        @Field("_api_key") apiKey: String?,
        @Field("buildType") buildType: String?
    ): DoPostEntity

    /*上传文件*/
    @Multipart
    @POST("apiv1/app/upload")
    suspend fun upload(
        @Part("uKey") uKey: RequestBody?,
        @Part("_api_key") apiKey: RequestBody?,
        @Part file: MultipartBody.Part
    ): UpLoadEntity

    /*下载*/
    @Streaming
    @GET
    suspend fun downLoad(@Header("RANGE") range: String?, @Url url: String?): ResponseBody
}