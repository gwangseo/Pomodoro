package com.app.pomodoro.ui.viewmodel

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.pomodoro.data.model.*
import com.app.pomodoro.data.repository.SessionRepository
import com.app.pomodoro.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.*

/**
 * 뽀모도로 타이머 관리 ViewModel
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sessionRepository = SessionRepository(application)
    private val userRepository = UserRepository(application)
    
    // LiveData들
    private val _timerState = MutableLiveData<TimerState>(TimerState.IDLE)
    val timerState: LiveData<TimerState> = _timerState
    
    private val _currentTime = MutableLiveData<Int>()
    val currentTime: LiveData<Int> = _currentTime
    
    private val _totalTime = MutableLiveData<Int>()
    val totalTime: LiveData<Int> = _totalTime
    
    private val _sessionType = MutableLiveData<SessionType>(SessionType.WORK)
    val sessionType: LiveData<SessionType> = _sessionType
    
    private val _progress = MutableLiveData<Float>(0f)
    val progress: LiveData<Float> = _progress
    
    private val _timerSettings = MutableLiveData<TimerSettings>()
    val timerSettings: LiveData<TimerSettings> = _timerSettings
    
    // 타이머 관련 변수들
    private var countDownTimer: CountDownTimer? = null
    private var startTime: Date? = null
    private var originalDuration: Int = 0
    
    init {
        loadTimerSettings()
    }
    
    /**
     * 타이머 설정 로드
     */
    private fun loadTimerSettings() {
        viewModelScope.launch {
            try {
                val settings = userRepository.getTimerSettings().getOrThrow()
                _timerSettings.value = settings
                setWorkSession(settings.workDuration * 60) // 분을 초로 변환
            } catch (e: Exception) {
                // 기본 설정 사용
                val defaultSettings = TimerSettings()
                _timerSettings.value = defaultSettings
                setWorkSession(defaultSettings.workDuration * 60)
            }
        }
    }
    
    /**
     * 작업 세션 설정
     */
    fun setWorkSession(durationInSeconds: Int = 0) {
        val duration = if (durationInSeconds > 0) {
            durationInSeconds
        } else {
            (_timerSettings.value?.workDuration ?: 25) * 60
        }
        
        _sessionType.value = SessionType.WORK
        _totalTime.value = duration
        _currentTime.value = duration
        originalDuration = duration
        _progress.value = 0f
        _timerState.value = TimerState.IDLE
    }
    
    /**
     * 휴식 세션 설정
     */
    fun setBreakSession() {
        val duration = (_timerSettings.value?.breakDuration ?: 5) * 60
        
        _sessionType.value = SessionType.BREAK
        _totalTime.value = duration
        _currentTime.value = duration
        originalDuration = duration
        _progress.value = 0f
        _timerState.value = TimerState.IDLE
    }
    
    /**
     * 타이머 시작
     */
    fun startTimer() {
        when (_timerState.value) {
            TimerState.IDLE -> {
                startTime = Date()
                startCountdown()
                _timerState.value = TimerState.RUNNING
            }
            TimerState.PAUSED -> {
                startCountdown()
                _timerState.value = TimerState.RUNNING
            }
            TimerState.RUNNING -> {
                // 이미 실행 중이면 일시정지
                pauseTimer()
            }
            else -> {
                // COMPLETED 상태에서는 아무 동작 안 함
            }
        }
    }
    
    /**
     * 타이머 일시정지
     */
    fun pauseTimer() {
        countDownTimer?.cancel()
        _timerState.value = TimerState.PAUSED
    }
    
    /**
     * 타이머 초기화
     */
    fun resetTimer() {
        countDownTimer?.cancel()
        
        val currentSessionType = _sessionType.value ?: SessionType.WORK
        if (currentSessionType == SessionType.WORK) {
            setWorkSession()
        } else {
            setBreakSession()
        }
    }
    
    /**
     * 타이머 완료
     */
    private fun completeTimer() {
        countDownTimer?.cancel()
        _timerState.value = TimerState.COMPLETED
        _currentTime.value = 0
        _progress.value = 1f
        
        // 세션 기록 저장
        saveSession(isCompleted = true)
        
        // 자동 세션 전환 (작업 -> 휴식 -> 작업)
        val currentSessionType = _sessionType.value ?: SessionType.WORK
        if (currentSessionType == SessionType.WORK) {
            // 작업 완료 후 휴식으로 전환
            setBreakSession()
        } else {
            // 휴식 완료 후 작업으로 전환
            setWorkSession()
        }
    }
    
    /**
     * 카운트다운 시작
     */
    private fun startCountdown() {
        val currentTimeValue = _currentTime.value ?: return
        val totalTimeValue = _totalTime.value ?: return
        
        countDownTimer = object : CountDownTimer((currentTimeValue * 1000L), 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                _currentTime.value = secondsRemaining
                
                // 진행률 계산
                val progressValue = if (totalTimeValue > 0) {
                    1f - (secondsRemaining.toFloat() / totalTimeValue.toFloat())
                } else {
                    0f
                }
                _progress.value = progressValue
            }
            
            override fun onFinish() {
                completeTimer()
            }
        }
        
        countDownTimer?.start()
    }
    
    /**
     * 세션 기록 저장
     */
    private fun saveSession(isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                val actualDuration = if (isCompleted) {
                    originalDuration / 60 // 초를 분으로 변환
                } else {
                    val remaining = _currentTime.value ?: 0
                    (originalDuration - remaining) / 60
                }
                
                val session = TimerSession(
                    duration = originalDuration / 60,
                    actualDuration = actualDuration,
                    isCompleted = isCompleted,
                    sessionType = _sessionType.value ?: SessionType.WORK,
                    startTime = startTime,
                    endTime = Date()
                )
                
                sessionRepository.saveSession(session)
            } catch (e: Exception) {
                // 로그 기록 또는 에러 처리
            }
        }
    }
    
    /**
     * 커스텀 시간 설정
     */
    fun setCustomTime(minutes: Int) {
        val seconds = minutes * 60
        _totalTime.value = seconds
        _currentTime.value = seconds
        originalDuration = seconds
        _progress.value = 0f
        _timerState.value = TimerState.IDLE
    }
    
    /**
     * 현재 세션 중단 및 저장
     */
    fun cancelCurrentSession() {
        if (_timerState.value == TimerState.RUNNING || _timerState.value == TimerState.PAUSED) {
            saveSession(isCompleted = false)
        }
        resetTimer()
    }
    
    /**
     * 시간 포맷팅 (MM:SS)
     */
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}