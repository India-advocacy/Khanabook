package com.khanabook.lite.pos.data.remote.interceptor

import com.khanabook.lite.pos.domain.manager.SessionManager
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor @Inject constructor(private val sessionManager: SessionManager) :
        Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val path = request.url.encodedPath

    // 1. Always apply ngrok bypass header to all requests (essential for free tier)
    val requestBuilder = request.newBuilder()
    requestBuilder.addHeader("ngrok-skip-browser-warning", "true")

    // 2. Skip adding the token for auth endpoints or external APIs like Meta/Facebook
    val isExternalApi = request.url.host.contains("facebook.com") || request.url.host.contains("google")
    
    if (!(path.contains("/auth") || path.contains("login") || isExternalApi)) {
        // 3. Fetch token blocking synchronously (interceptors run on background threads)
        val token = sessionManager.getAuthToken()

        // 4. Stricter validation: JWT must contain dots. Prevents "null" or empty string spam.
        if (!token.isNullOrBlank() && token != "null" && token.contains(".")) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
    }

    return chain.proceed(requestBuilder.build())
  }
}
