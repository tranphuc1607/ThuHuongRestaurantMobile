package com.example.thuhuong_restaurant.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thuhuong_restaurant.core.common.ApiResult
import com.example.thuhuong_restaurant.core.model.UserResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SessionStatus { LOADING, GUEST, AUTHENTICATED }

data class LoginFormState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

data class RegisterFormState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

/**
 * App-wide session holder (role-aware nav depends on [currentUser]/[status]) doubling as the
 * view-model for the login/register forms. Mirrors AuthContext.tsx on the web front-end.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()

    private val _status = MutableStateFlow(SessionStatus.LOADING)
    val status: StateFlow<SessionStatus> = _status.asStateFlow()

    var loginState by mutableStateOf(LoginFormState())
        private set

    var registerState by mutableStateOf(RegisterFormState())
        private set

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val hasToken = authRepository.session.first().accessToken != null
            if (!hasToken) {
                _status.value = SessionStatus.GUEST
                return@launch
            }
            when (val result = authRepository.fetchMe()) {
                is ApiResult.Success -> {
                    _currentUser.value = result.data
                    _status.value = SessionStatus.AUTHENTICATED
                }
                is ApiResult.Failure -> {
                    _currentUser.value = null
                    _status.value = SessionStatus.GUEST
                }
            }
        }
    }

    fun onLoginUsernameChange(value: String) {
        loginState = loginState.copy(username = value, error = null)
    }

    fun onLoginPasswordChange(value: String) {
        loginState = loginState.copy(password = value, error = null)
    }

    fun submitLogin(onSuccess: () -> Unit) {
        val state = loginState
        if (state.username.isBlank() || state.password.isBlank()) {
            loginState = state.copy(error = "Vui lòng nhập đầy đủ thông tin")
            return
        }
        viewModelScope.launch {
            loginState = loginState.copy(isSubmitting = true, error = null)
            when (val result = authRepository.login(state.username.trim(), state.password)) {
                is ApiResult.Success -> {
                    _currentUser.value = result.data
                    _status.value = SessionStatus.AUTHENTICATED
                    loginState = LoginFormState()
                    onSuccess()
                }
                is ApiResult.Failure -> {
                    loginState = loginState.copy(isSubmitting = false, error = result.message)
                }
            }
        }
    }

    fun onRegisterUsernameChange(value: String) {
        registerState = registerState.copy(username = value, error = null)
    }

    fun onRegisterPasswordChange(value: String) {
        registerState = registerState.copy(password = value, error = null)
    }

    fun onRegisterConfirmPasswordChange(value: String) {
        registerState = registerState.copy(confirmPassword = value, error = null)
    }

    fun submitRegister() {
        val state = registerState
        if (state.username.isBlank() || state.password.isBlank()) {
            registerState = state.copy(error = "Vui lòng nhập đầy đủ thông tin")
            return
        }
        if (state.password != state.confirmPassword) {
            registerState = state.copy(error = "Mật khẩu xác nhận không khớp")
            return
        }
        viewModelScope.launch {
            registerState = registerState.copy(isSubmitting = true, error = null)
            when (val result = authRepository.register(state.username.trim(), state.password)) {
                is ApiResult.Success ->
                    registerState = registerState.copy(isSubmitting = false, success = true)
                is ApiResult.Failure ->
                    registerState = registerState.copy(isSubmitting = false, error = result.message)
            }
        }
    }

    fun resetRegisterState() {
        registerState = RegisterFormState()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _currentUser.value = null
            _status.value = SessionStatus.GUEST
        }
    }
}
