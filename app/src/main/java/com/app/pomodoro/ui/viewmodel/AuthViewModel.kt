package com.app.pomodoro.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.pomodoro.data.repository.AuthRepository
import com.app.pomodoro.data.repository.CloudAchievementsRepository
import com.app.pomodoro.data.repository.SessionRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

/**
 * 인증 상태를 관리하는 ViewModel
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository(application)
    private val cloudRepository = CloudAchievementsRepository()
    private val sessionRepository = SessionRepository(application)
    
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    
    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser
    
    init {
        checkAuthState()
    }
    
    /**
     * 현재 인증 상태 확인
     */
    private fun checkAuthState() {
        val user = authRepository.getCurrentUser()
        _currentUser.value = user
        
        if (user != null) {
            _authState.value = AuthState.SIGNED_IN
            // 사용자 정보를 클라우드에 저장
            viewModelScope.launch {
                user.email?.let { email ->
                    user.displayName?.let { name ->
                        cloudRepository.saveUserInfo(user.uid, email, name)
                    }
                }
                // SessionRepository에도 사용자 ID 저장
                sessionRepository.saveUserId(user.uid)
            }
        } else {
            _authState.value = AuthState.SIGNED_OUT
        }
    }
    
    /**
     * Google 로그인 클라이언트 반환
     */
    fun getGoogleSignInClient() = authRepository.getGoogleSignInClient()
    
    /**
     * Google 계정으로 로그인
     */
    fun signInWithGoogle(account: GoogleSignInAccount) {
        android.util.Log.d("AuthViewModel", "Google 로그인 시작: ${account.email}")
        _authState.value = AuthState.LOADING
        
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(account)
            
            result.fold(
                onSuccess = { user ->
                    android.util.Log.d("AuthViewModel", "Google 로그인 성공: ${user.email}")
                    _currentUser.value = user
                    _authState.value = AuthState.SIGNED_IN
                    
                    // 사용자 정보를 클라우드에 저장
                    user.email?.let { email ->
                        user.displayName?.let { name ->
                            android.util.Log.d("AuthViewModel", "사용자 정보 클라우드 저장 시작: $email")
                            cloudRepository.saveUserInfo(user.uid, email, name)
                        }
                    }
                    
                    // SessionRepository에도 사용자 ID 저장
                    android.util.Log.d("AuthViewModel", "SessionRepository에 사용자 ID 저장: ${user.uid}")
                    sessionRepository.saveUserId(user.uid)
                },
                onFailure = { exception ->
                    android.util.Log.e("AuthViewModel", "Google 로그인 실패: ${exception.message}", exception)
                    _authState.value = AuthState.ERROR(exception.message ?: "로그인 실패")
                }
            )
        }
    }
    
    /**
     * 로그아웃
     */
    fun signOut() {
        _authState.value = AuthState.LOADING
        
        viewModelScope.launch {
            val result = authRepository.signOut()
            
            result.fold(
                onSuccess = {
                    _currentUser.value = null
                    _authState.value = AuthState.SIGNED_OUT
                    // SessionRepository에서 사용자 ID 삭제
                    sessionRepository.clearUserId()
                },
                onFailure = { exception ->
                    _authState.value = AuthState.ERROR(exception.message ?: "로그아웃 실패")
                }
            )
        }
    }
    
    /**
     * 현재 사용자 ID 반환
     */
    fun getCurrentUserId(): String? = authRepository.getUserId()
    
    /**
     * 현재 사용자 이메일 반환
     */
    fun getCurrentUserEmail(): String? = authRepository.getUserEmail()
    
    /**
     * 현재 사용자 이름 반환
     */
    fun getCurrentUserName(): String? = authRepository.getUserName()
}

/**
 * 인증 상태를 나타내는 sealed class
 */
sealed class AuthState {
    object LOADING : AuthState()
    object SIGNED_IN : AuthState()
    object SIGNED_OUT : AuthState()
    data class ERROR(val message: String) : AuthState()
}
