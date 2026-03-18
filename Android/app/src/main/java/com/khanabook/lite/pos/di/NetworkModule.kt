package com.khanabook.lite.pos.di

import com.khanabook.lite.pos.BuildConfig
import com.khanabook.lite.pos.data.remote.WhatsAppApiService
import com.khanabook.lite.pos.data.remote.api.KhanaBookApi
import com.khanabook.lite.pos.data.remote.api.NvidiaNimApiService
import com.khanabook.lite.pos.data.remote.interceptor.AuthInterceptor
import com.khanabook.lite.pos.domain.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val META_BASE_URL = "https://graph.facebook.com/v17.0/"
    private const val NVIDIA_BASE_URL = "https://integrate.api.nvidia.com/v1/"
    private const val BACKEND_BASE_URL = BuildConfig.BACKEND_URL

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("AuthOkHttpClient")
    fun provideAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("NvidiaOkHttpClient")
    fun provideNvidiaOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("MetaRetrofit")
    fun provideMetaRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(META_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @Named("NvidiaRetrofit")
    fun provideNvidiaRetrofit(@Named("NvidiaOkHttpClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NVIDIA_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @Named("BackendRetrofit")
    fun provideBackendRetrofit(@Named("AuthOkHttpClient") okHttpClient: OkHttpClient): Retrofit {
        val baseUrlWithPrefix = if (BACKEND_BASE_URL.endsWith("/")) {
            BACKEND_BASE_URL + "api/v1/"
        } else {
            BACKEND_BASE_URL + "/api/v1/"
        }
        return Retrofit.Builder()
            .baseUrl(baseUrlWithPrefix)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideWhatsAppApiService(@Named("MetaRetrofit") retrofit: Retrofit): WhatsAppApiService {
        return retrofit.create(WhatsAppApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNvidiaNimApiService(@Named("NvidiaRetrofit") retrofit: Retrofit): NvidiaNimApiService {
        return retrofit.create(NvidiaNimApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideKhanaBookApi(@Named("BackendRetrofit") retrofit: Retrofit): KhanaBookApi {
        return retrofit.create(KhanaBookApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: android.content.Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
}
