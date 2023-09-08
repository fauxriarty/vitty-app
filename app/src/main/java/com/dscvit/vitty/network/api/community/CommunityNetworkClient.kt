package com.dscvit.vitty.network.api.community


import com.dscvit.vitty.util.APIConstants.Community_BASE_URL
import com.dscvit.vitty.util.APIConstants.TIMEOUT
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object CommunityNetworkClient {

    var retrofit: Retrofit? = null

    val retrofitClientCommunity: Retrofit
        get() {
            if (retrofit == null) {
                val okHttpClientBuilder = OkHttpClient.Builder()
                okHttpClientBuilder.connectTimeout(TIMEOUT.toLong(), TimeUnit.SECONDS)
                retrofit = Retrofit.Builder()
                    .baseUrl(Community_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClientBuilder.build())
                    .build()
            }
            return retrofit!!
        }
}