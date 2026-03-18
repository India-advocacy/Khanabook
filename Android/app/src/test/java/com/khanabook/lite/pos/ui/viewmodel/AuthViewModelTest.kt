package com.khanabook.lite.pos.ui.viewmodel

import com.khanabook.lite.pos.data.local.entity.UserEntity
import com.khanabook.lite.pos.data.remote.WhatsAppApiService
import com.khanabook.lite.pos.data.repository.RestaurantRepository
import com.khanabook.lite.pos.data.repository.UserRepository
import com.khanabook.lite.pos.domain.manager.AuthManager
import com.khanabook.lite.pos.domain.manager.SessionManager
import com.khanabook.lite.pos.domain.manager.SyncManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val whatsAppApiService: WhatsAppApiService = mockk(relaxed = true)
    private val restaurantRepository: RestaurantRepository = mockk(relaxed = true)
    private val syncManager: SyncManager = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val authManager: AuthManager = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { userRepository.currentUser } returns MutableStateFlow(null)

        viewModel = AuthViewModel(
            userRepository,
            whatsAppApiService,
            restaurantRepository,
            syncManager,
            sessionManager,
            authManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `login returns Success via local fallback when remote login fails and credentials are correct`() = runTest {
        val email = "9150677849"
        val password = "owner123"
        val hashedPassword = "\$2a\$12\$SomeBcryptHash.Fake"
        val localHash = "computed-local-hash"

        val fakeUser = UserEntity(
            id = 1,
            name = "Owner",
            email = email,
            passwordHash = hashedPassword,
            restaurantId = 1,
            deviceId = "device",
            isActive = true,
            isSynced = false,
        )

        coEvery { authManager.hashPassword(password) } returns localHash
        coEvery { userRepository.remoteLogin(email, password, localHash) } returns Result.failure(Exception("Network Error"))
        coEvery { userRepository.getUserByEmail(email) } returns fakeUser
        coEvery { authManager.verifyPassword(password, hashedPassword) } returns true

        viewModel.login(email, password)
        advanceUntilIdle()

        val result = viewModel.loginStatus.value
        assertTrue("Expected login to succeed via local fallback", result is AuthViewModel.LoginResult.Success)
        coVerify(exactly = 1) { userRepository.setCurrentUser(fakeUser) }

        val successResult = result as AuthViewModel.LoginResult.Success
        assertEquals(fakeUser.email, successResult.user.email)
    }

    @Test
    fun `login returns Error when remote fails and local credentials do not match`() = runTest {
        val email = "9150677849"
        val wrongPassword = "wrongPassword123"
        val hashedPassword = "\$2a\$12\$SomeBcryptHash.Fake"
        val localHash = "computed-local-hash"

        val fakeUser = UserEntity(
            id = 1,
            name = "Owner",
            email = email,
            passwordHash = hashedPassword,
            restaurantId = 1,
            deviceId = "device",
            isActive = true,
            isSynced = false,
            createdAt = System.currentTimeMillis()
        )

        coEvery { authManager.hashPassword(wrongPassword) } returns localHash
        coEvery { userRepository.remoteLogin(email, wrongPassword, localHash) } returns Result.failure(Exception("Network Error"))
        coEvery { userRepository.getUserByEmail(email) } returns fakeUser
        coEvery { authManager.verifyPassword(wrongPassword, hashedPassword) } returns false

        viewModel.login(email, wrongPassword)
        advanceUntilIdle()

        val result = viewModel.loginStatus.value
        assertTrue("Expected login to fail", result is AuthViewModel.LoginResult.Error)

        val errorResult = result as AuthViewModel.LoginResult.Error
        assertEquals(AuthViewModel.LoginErrorCode.INCORRECT_PASSWORD, errorResult.code)
        assertTrue(errorResult.message.startsWith("Incorrect password."))
        assertTrue(errorResult.message.contains("attempt(s) remaining"))
    }
}
