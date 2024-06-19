package io.agora.ktvdemo.api

import android.util.Log
import com.moczul.ok2curl.CurlInterceptor
import com.moczul.ok2curl.logger.Logger
import io.agora.ktvdemo.BuildConfig
import io.agora.ktvdemo.MyApplication
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 云端合流请求
 */
class CloudApiManager private constructor() {

    companion object {
        fun getInstance(): CloudApiManager {
            return InstanceHolder.apiManager
        }

        //private const val testIp = "218.205.37.50"
        private const val domain = "https://api.sd-rtn.com"
        private const val TAG = "ApiManager"
        private const val cloudRtcUid = 20232023
    }

    internal object InstanceHolder {
        val apiManager = CloudApiManager()
    }

    private var tokenName = ""
    private var taskId = ""
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .addInterceptor(CurlInterceptor(object : Logger {
            override fun log(message: String) {
                Log.d(TAG, message)
            }
        }))
        .build()

    fun fetchStartCloud(mainChannel: String) {
        var taskId = ""
        try {
            val transcoderObj = JSONObject()
            val inputRetObj = JSONObject()
                .put("rtcUid", 0)
                .put("rtcChannel", mainChannel)


            val outputRetObj = JSONObject()
                .put("rtcUid", cloudRtcUid)
                .put("rtcChannel", mainChannel + "_ad")

            transcoderObj.put("appId", BuildConfig.AGORA_APP_ID)
            transcoderObj.put("appCert", BuildConfig.AGORA_APP_CERTIFICATE)
            transcoderObj.put("src", "Android")
            transcoderObj.put("traceId", "12345")
            transcoderObj.put("instanceId", System.currentTimeMillis().toString())
            transcoderObj.put("basicAuth", "")
            transcoderObj.put("audioInputsRtc", inputRetObj)
            transcoderObj.put("outputsRtc", outputRetObj)

            val request: Request = Builder()
                .url(startTaskUrl())
                .addHeader("Content-Type", "application/json")
                .post(transcoderObj.toString().toRequestBody())
                .build()

            Log.d(TAG, "fetchStartCloud: ${request.url}")

            val responseStart = okHttpClient.newCall(request).execute()
            if (responseStart.isSuccessful) {
                val body = responseStart.body!!
                val bodyString = body.string()
                val jsonUid = JSONObject(bodyString).get("data") as JSONObject

                if (jsonUid.has("taskId")) {
                    taskId = jsonUid.getString("taskId")
                }
                if (jsonUid.has("builderToken")) {
                    tokenName = jsonUid.getString("builderToken")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "云端合流uid 请求报错 " + e.message)
        }
        if (taskId.isNotEmpty()) {
            this.taskId = taskId
        }
    }

    fun fetchStopCloud() {
        if (taskId.isEmpty() || tokenName.isEmpty()) {
            Log.e(TAG, "云端合流任务停止失败 taskId || tokenName is null")
            return
        }
        try {
            val transcoderObj = JSONObject()
            transcoderObj.put("appId", BuildConfig.AGORA_APP_ID)
            transcoderObj.put("appCert", BuildConfig.AGORA_APP_CERTIFICATE)
            transcoderObj.put("src", "Android")
            transcoderObj.put("traceId", "12345")
            transcoderObj.put("basicAuth", "")
            transcoderObj.put("taskId", taskId)
            transcoderObj.put("builderToken", tokenName)

            val request: Request = Builder()
                .url(deleteTaskUrl())
                .addHeader("Content-Type", "application/json")
                .delete(transcoderObj.toString().toRequestBody())
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body!!
                val bodyString = body.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "云端合流任务停止失败 " + e.message)
        }
    }

    private fun startTaskUrl(): String {
        val domain = BuildConfig.TOOLBOX_SERVER_HOST
        return String.format("%s/v1/cloud-transcoder/start", domain)
    }

    private fun deleteTaskUrl(): String {
        val domain = BuildConfig.TOOLBOX_SERVER_HOST
        return String.format("%s/v1/cloud-transcoder/stop", domain)
    }
}