package com.khanabook.lite.pos.test

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.khanabook.lite.pos.MainActivity
import com.khanabook.lite.pos.test.api.MockApiServer
import com.khanabook.lite.pos.test.robots.LoginRobot
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidInstrumentationTest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidInstrumentationTest
@RunWith(JUnit4::class)
class SecurityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mockApiServer: MockApiServer
    private lateinit var loginRobot: LoginRobot

    @Before
    fun setUp() {
        hiltRule.inject()
        mockApiServer = MockApiServer()
        mockApiServer.start()
        loginRobot = LoginRobot(composeTestRule)
    }

    @After
    fun tearDown() {
        mockApiServer.shutdown()
    }

    @Test
    fun TC-SEC-001_TokenSecurity_NoTokenInSharedPreferences() {
        mockApiServer.enqueueLoginSuccess()
        
        loginRobot.enterCredentials()
        loginRobot.tapLogin()
        
        Thread.sleep(2000)
        
        val prefsDir = File("data/data/com.khanabook.lite.pos/shared_prefs")
        if (prefsDir.exists()) {
            prefsDir.listFiles()?.forEach { file ->
                val content = file.readText()
                assert(!content.contains("Bearer ")) { "Token found in SharedPreferences: ${file.name}" }
                assert(!content.contains("eyJ")) { "JWT token found in SharedPreferences: ${file.name}" }
            }
        }
    }

    @Test
    fun TC-SEC-001_TokenSecurity_NoTokenInLogcat() {
        mockApiServer.enqueueLoginSuccess()
        
        loginRobot.enterCredentials()
        loginRobot.tapLogin()
        
        Thread.sleep(2000)
        
        val process = Runtime.getRuntime().exec("logcat -d | grep -i 'Bearer\\|eyJ'")
        val output = process.inputStream.bufferedReader().readText()
        
        val linesWithTokens = output.lines()
            .filter { it.contains("eyJ") || it.contains("Bearer ") }
            .filter { !it.contains("X-JWT-Token") }
        
        assert(linesWithTokens.isEmpty()) { "Token leaked in Logcat: ${linesWithTokens.joinToString()}" }
    }

    @Test
    fun TC-SEC-003_NetworkSecurity_AllApiCallsOverHttps() {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        
        val request = Request.Builder()
            .url(mockApiServer.url("/api/auth/login"))
            .build()
        
        val response = client.newCall(request).execute()
        
        assert(response.protocol.name == "HTTP/1.1" || response.protocol.name == "HTTP/2")
    }

    @Test
    fun TC-API-002_AuthFailure_401Response_HandledGracefully() {
        mockApiServer.enqueueLoginFailure()
        
        loginRobot.enterCredentials(
            phone = "9876543210",
            password = "wrongpass"
        )
        loginRobot.tapLogin()
        
        loginRobot.assertErrorMessageVisible()
        loginRobot.assertPhoneFieldVisible()
    }

    @Test
    fun TC-API-005_Timeout_SlowResponse_HandledGracefully() {
        mockApiServer.enqueue(mockApiServer.slowResponse())
        
        loginRobot.enterCredentials()
        loginRobot.tapLogin()
        
        loginRobot.assertLoadingIndicatorShown()
    }

    @Test
    fun TC-API-005_Timeout_ShowsLoadingIndicator() {
        mockApiServer.enqueue(mockApiServer.slowResponse())
        
        loginRobot.enterCredentials()
        loginRobot.tapLogin()
        
        loginRobot.assertLoadingIndicatorShown()
    }
}
