package com.app.pomodoro.data.repository

import android.content.Context
import android.content.Intent
import com.app.pomodoro.data.model.UserProfile
import com.app.pomodoro.data.model.TimerSettings

/**
 * 사용자 인증 및 프로필 관리 Repository
 * 현재는 로컬 저장소만 사용 (Firebase 설정 완료 후 확장 예정)
 */
class UserRepository(private val context: Context) {
    
    private val sharedPrefs = context.getSharedPreferences("pomodoro_user", Context.MODE_PRIVATE)
    
    companion object {
        const val RC_SIGN_IN = 9001
        private const val PREF_TIMER_SETTINGS = "timer_settings"
        private const val PREF_WORK_DURATION = "work_duration"
        private const val PREF_BREAK_DURATION = "break_duration"
        private const val PREF_NOTIFICATIONS = "notifications"
        private const val PREF_VIBRATION = "vibration"
        private const val PREF_SOUND = "sound"
    }
    
    /**
     * 현재 로그인된 사용자 정보 반환 (현재는 null)
     */
    fun getCurrentUser(): Any? {
        return null // Firebase 설정 완료 후 구현
    }
    
    /**
     * 로그인 상태 확인 (현재는 항상 false)
     */
    fun isLoggedIn(): Boolean {
        return false // Firebase 설정 완료 후 구현
    }
    
    /**
     * Google 로그인 Intent 반환 (현재는 빈 Intent)
     */
    fun getGoogleSignInIntent(): Intent {
        return Intent() // Firebase 설정 완료 후 구현
    }
    
    /**
     * Google 로그인 결과 처리 (현재는 실패 반환)
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Result<UserProfile> {
        return Result.failure(Exception("Firebase Auth is not configured"))
    }
    
    /**
     * 로그아웃 (현재는 성공 반환)
     */
    suspend fun signOut(): Result<Unit> {
        return Result.success(Unit)
    }
    
    /**
     * 사용자 프로필 저장 (현재는 성공 반환)
     */
    suspend fun saveUserProfile(userProfile: UserProfile): Result<Unit> {
        return Result.success(Unit)
    }
    
    /**
     * 사용자 프로필 조회 (현재는 기본 프로필 반환)
     */
    suspend fun getUserProfile(uid: String): Result<UserProfile> {
        return Result.success(UserProfile(uid = uid))
    }
    
    /**
     * 타이머 설정 업데이트
     */
    suspend fun updateTimerSettings(settings: TimerSettings): Result<Unit> {
        return try {
            sharedPrefs.edit().apply {
                putInt(PREF_WORK_DURATION, settings.workDuration)
                putInt(PREF_BREAK_DURATION, settings.breakDuration)
                putBoolean(PREF_NOTIFICATIONS, settings.enableNotifications)
                putBoolean(PREF_VIBRATION, settings.enableVibration)
                putBoolean(PREF_SOUND, settings.enableSound)
                apply()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 타이머 설정 조회
     */
    suspend fun getTimerSettings(): Result<TimerSettings> {
        return try {
            val settings = TimerSettings(
                workDuration = sharedPrefs.getInt(PREF_WORK_DURATION, 25),
                breakDuration = sharedPrefs.getInt(PREF_BREAK_DURATION, 5),
                enableNotifications = sharedPrefs.getBoolean(PREF_NOTIFICATIONS, true),
                enableVibration = sharedPrefs.getBoolean(PREF_VIBRATION, true),
                enableSound = sharedPrefs.getBoolean(PREF_SOUND, true)
            )
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}