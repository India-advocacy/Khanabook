package com.khanabook.lite.pos.di

import com.khanabook.lite.pos.data.remote.WhatsAppApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val META_BASE_URL = "https://graph.facebook.com/v17.0/"
    private const val BACKEND_BASE_URL = com.khanabook.lite.pos.BuildConfig.BACKEND_URL

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (com.khanabook.lite.pos.BuildConfig.DEBUG) {
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
        authInterceptor: com.khanabook.lite.pos.data.remote.interceptor.AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @javax.inject.Named("AuthOkHttpClient")
    fun provideAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: com.khanabook.lite.pos.data.remote.interceptor.AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @javax.inject.Named("MetaRetrofit")
    fun provideMetaRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(META_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @javax.inject.Named("BackendRetrofit")
    fun provideBackendRetrofit(@javax.inject.Named("AuthOkHttpClient") okHttpClient: OkHttpClient): Retrofit {
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
    fun provideWhatsAppApiService(@javax.inject.Named("MetaRetrofit") retrofit: Retrofit): WhatsAppApiService {
        return retrofit.create(WhatsAppApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideKhanaBookApi(@javax.inject.Named("BackendRetrofit") retrofit: Retrofit): com.khanabook.lite.pos.data.remote.api.KhanaBookApi {
        return retrofit.create(com.khanabook.lite.pos.data.remote.api.KhanaBookApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): com.khanabook.lite.pos.domain.util.NetworkMonitor {
        return com.khanabook.lite.pos.domain.util.NetworkMonitor(context)
    }
}


