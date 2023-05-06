package com.bhm.flowhttp.body

import android.annotation.SuppressLint
import androidx.lifecycle.lifecycleScope
import com.bhm.flowhttp.core.HttpBuilder
import com.bhm.flowhttp.core.callback.UploadCallBack
import com.bhm.flowhttp.define.CommonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*
import java.io.IOException

/**
 * Created by bhm on 2023/5/6.
 */
class UploadRequestBody(private val mRequestBody: RequestBody, private val httpBuilder: HttpBuilder?) :
    RequestBody() {
    override fun contentType(): MediaType? {
        return mRequestBody.contentType()
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return try {
            mRequestBody.contentLength()
        } catch (e: IOException) {
            e.printStackTrace()
            -1
        }
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        if (sink is Buffer
            || sink.toString().contains(
                "com.android.tools.profiler.support.network.HttpTracker\$OutputStreamTracker")) {
            mRequestBody.writeTo(sink)
        } else {
            val bufferedSink: BufferedSink
            val mCountingSink = CountingSink(sink)
            bufferedSink = mCountingSink.buffer()
            mRequestBody.writeTo(bufferedSink)
            bufferedSink.flush()
            bufferedSink.close()
        }
    }

    internal inner class CountingSink(delegate: Sink?) : ForwardingSink(delegate!!) {
        private var bytesWritten = 0L
        private var contentLength = 0L
        @SuppressLint("CheckResult")
        @Throws(IOException::class)
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            httpBuilder?.callBack?.let { callBack ->
                if (callBack is UploadCallBack<*>) {
                    if (contentLength == 0L) {
                        contentLength = contentLength()
                    }
                    if (bytesWritten == 0L) {
                        httpBuilder.activity.lifecycleScope.launch(Dispatchers.Main) {
                            CommonUtil.logger(httpBuilder, "upLoad-- > ", "begin upLoad")
                        }
                    }
                    bytesWritten += byteCount
                    val progress = (bytesWritten * 100 / contentLength).toInt()
                    httpBuilder.activity.lifecycleScope.launch(Dispatchers.Main) {
                        callBack.onProgress(
                            if (progress > 100) 100 else progress,
                            byteCount,
                            contentLength
                        )
                    }
                }
            }
        }
    }
}