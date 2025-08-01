package com.app.pomodoro.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 뽀모도로 타이머 세션 기록 데이터 클래스
 */
data class TimerSession(
    val id: String = "",
    val duration: Int = 0, // 설정된 시간 (분)
    val actualDuration: Int = 0, // 실제 완료된 시간 (분)
    val isCompleted: Boolean = false, // 완료 여부
    val sessionType: SessionType = SessionType.WORK,
    @ServerTimestamp
    val startTime: Date? = null,
    @ServerTimestamp
    val endTime: Date? = null,
    val userId: String? = null // Firebase Auth UID
)

enum class SessionType {
    WORK, BREAK
}

/**
 * 타이머 상태
 */
enum class TimerState {
    IDLE,      // 시작 전
    RUNNING,   // 실행 중
    PAUSED,    // 일시정지
    COMPLETED  // 완료
}

/**
 * 타이머 설정 정보
 */
data class TimerSettings(
    val workDuration: Int = 25, // 작업 시간 (분)
    val breakDuration: Int = 5,  // 휴식 시간 (분)
    val enableNotifications: Boolean = true,
    val enableVibration: Boolean = true,
    val enableSound: Boolean = true
)

/**
 * 사용자 프로필 정보
 */
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val settings: TimerSettings = TimerSettings()
)

/**
 * 통계 데이터
 */
data class SessionStats(
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val totalWorkTime: Int = 0, // 총 작업 시간 (분)
    val averageSessionLength: Double = 0.0,
    val completionRate: Double = 0.0
)