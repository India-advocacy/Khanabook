package com.khanabook.lite.pos.test

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.compose.ui.test.junit4.AndroidComposeUiTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.khanabook.lite.pos.MainActivity
import com.khanabook.lite.pos.test.api.MockApiServer
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.File

abstract class BaseTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    protected lateinit var mockApiServer: MockApiServer
    protected lateinit var uiDevice: UiDevice

    @Before
    open fun setUp() {
        hiltRule.inject()
        
        mockApiServer = MockApiServer()
        mockApiServer.start()
        
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @After
    open fun tearDown() {
        if (::mockApiServer.isInitialized) {
            mockApiServer.shutdown()
        }
        enableNetwork()
    }

    protected fun enableNetwork() {
        executeShellCommand("svc wifi enable")
        executeShellCommand("svc data enable")
    }

    protected fun disableNetwork() {
        executeShellCommand("svc wifi disable")
        executeShellCommand("svc data disable")
    }

    protected fun executeShellCommand(command: String) {
        try {
            Runtime.getRuntime().exec(command)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun rotateDevice(orientation: ScreenOrientation) {
        val activity = composeTestRule.activity
        activity.requestedOrientation = when (orientation) {
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ScreenOrientation.REVERSE_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
    }

    protected fun clearAppData() {
        executeShellCommand("pm clear com.khanabook.lite.pos")
    }

    protected fun forceStopApp() {
        executeShellCommand("am force-stop com.khanabook.lite.pos")
    }

    protected fun restartApp() {
        forceStopApp()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ApplicationProvider.getApplicationContext<android.app.Application>().startActivity(intent)
    }

    protected fun captureScreenshot(name: String) {
        val screenshotsDir = File("/sdcard/screenshots")
        screenshotsDir.mkdirs()
        uiDevice.takeScreenshot(File(screenshotsDir, "${name}_${System.currentTimeMillis()}.png"))
    }

    protected fun pressBackKey() {
        uiDevice.pressBack()
    }

    protected fun pressHomeKey() {
        uiDevice.pressHome()
    }

    protected fun pressRecentApps() {
        uiDevice.pressRecentApps()
    }

    protected fun waitForIdle(timeoutMs: Long = 3000) {
        uiDevice.waitForIdle(timeoutMs)
    }

    protected fun setLocale(language: String, country: String = "") {
        executeShellCommand("setprop persist.sys.locale $language-$country")
        executeShellCommand("setprop ctl.restart zygote")
    }

    protected fun setFontScale(scale: Float) {
        executeShellCommand("settings put font_scale $scale")
    }

    protected fun setDisplaySize(size: Int) {
        executeShellCommand("settings put display_size_forced $size")
    }

    protected fun resetDisplaySettings() {
        executeShellCommand("settings delete display_size_forced")
        executeShellCommand("settings put font_scale 1.0")
    }

    enum class ScreenOrientation {
        PORTRAIT, LANDSCAPE, REVERSE_LANDSCAPE
    }
}
