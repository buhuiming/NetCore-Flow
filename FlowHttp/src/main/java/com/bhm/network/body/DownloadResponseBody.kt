package com.bhm.network.body

import android.annotation.SuppressLint
import androidx.lifecycle.lifecycleScope
import com.bhm.network.core.HttpOptions
import com.bhm.network.core.callback.DownloadCallBack
import com.bhm.network.define.CommonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*
import java.io.IOException

/** 下载请求体
 * Created by bhm on 2023/5/6.
 */
class DownloadResponseBody(
    private val responseBody: ResponseBody,
    private val httpOptions: HttpOptions
) : ResponseBody() {
    // BufferedSource 是okio库中的输入流，这里就当作inputStream来使用。
    private var bufferedSource: BufferedSource? = null
    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource as BufferedSource
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = httpOptions.writtenLength()
            val totalBytes = httpOptions.writtenLength() + responseBody.contentLength()

            @SuppressLint("CheckResult")
            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                if (httpOptions.callBack == null || httpOptions.callBack !is DownloadCallBack) {
                    return super.read(sink, byteCount)
                }
                val bytesRead = super.read(sink, byteCount)
                val callBack = httpOptions.callBack as DownloadCallBack
                if (totalBytesRead == 0L && bytesRead != -1L) {
                    CommonUtil.deleteFile(httpOptions, totalBytes)
                    if (httpOptions.isLogOutPut) {
                        httpOptions.activity.lifecycleScope.launch(Dispatchers.Main) {
                            CommonUtil.logger(httpOptions, "DownLoad-- > ", "begin downLoad")
                        }
                    }
                }
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                if (bytesRead != -1L) {
                    val progress = (totalBytesRead * 100 / totalBytes).toInt()
                    httpOptions.activity.lifecycleScope.launch(Dispatchers.Main) {
                        callBack.onProgress(
                            if (progress > 100) 100 else progress,
                            bytesRead,
                            totalBytes
                        )
                    }
                    if (totalBytesRead == totalBytes) {
                        httpOptions.activity.lifecycleScope.launch(Dispatchers.Main) {
                            callBack.onProgress(100, bytesRead, totalBytes)
                            CommonUtil.logger(httpOptions, "DownLoad-- > ", "finish downLoad")
                            if (null != httpOptions.dialog && httpOptions.isShowDialog) {
                                httpOptions.dialog?.dismissLoading(httpOptions.activity)
                            }
                        }
                    }
                }
                CommonUtil.writeFile(sink.inputStream(), httpOptions)
                return bytesRead
            }
        }
    }
}