package com.app.pomodoro.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.app.pomodoro.data.model.TimerSession
import com.app.pomodoro.data.model.SessionStats
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 세션 데이터를 관리하는 Repository
 * 현재는 로컬 SharedPreferences만 사용 (Firebase 설정 완료 후 확장 예정)
 */
class SessionRepository(private val context: Context) {
    
    private val sharedPrefs = context.getSharedPreferences("pomodoro_sessions", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val LOCAL_SESSIONS_KEY = "local_sessions"
    }
    
    /**
     * 새로운 세션 저장
     */
    suspend fun saveSession(session: TimerSession): Result<String> {
        return try {
            val sessionWithId = session.copy(id = generateLocalId())
            saveSessionLocally(sessionWithId)
            Result.success(sessionWithId.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 사용자의 모든 세션 조회
     */
    suspend fun getAllSessions(): Result<List<TimerSession>> {
        return try {
            val sessions = getLocalSessions()
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 완료된 세션만 조회
     */
    suspend fun getCompletedSessions(): Result<List<TimerSession>> {
        return try {
            val allSessions = getAllSessions().getOrThrow()
            val completedSessions = allSessions.filter { it.isCompleted }
            Result.success(completedSessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 중단된 세션만 조회
     */
    suspend fun getCancelledSessions(): Result<List<TimerSession>> {
        return try {
            val allSessions = getAllSessions().getOrThrow()
            val cancelledSessions = allSessions.filter { !it.isCompleted }
            Result.success(cancelledSessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 통계 데이터 계산
     */
    suspend fun getSessionStats(): Result<SessionStats> {
        return try {
            val allSessions = getAllSessions().getOrThrow()
            val totalSessions = allSessions.size
            val completedSessions = allSessions.count { it.isCompleted }
            val totalWorkTime = allSessions.filter { it.isCompleted }.sumOf { it.actualDuration }
            val averageSessionLength = if (completedSessions > 0) {
                allSessions.filter { it.isCompleted }.map { it.actualDuration }.average()
            } else 0.0
            val completionRate = if (totalSessions > 0) {
                completedSessions.toDouble() / totalSessions.toDouble() * 100
            } else 0.0
            
            val stats = SessionStats(
                totalSessions = totalSessions,
                completedSessions = completedSessions,
                totalWorkTime = totalWorkTime,
                averageSessionLength = averageSessionLength,
                completionRate = completionRate
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 세션 삭제
     */
    suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            deleteSessionLocally(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 로컬 저장 관련 private 메서드들
    
    private fun saveSessionLocally(session: TimerSession) {
        val sessions = getLocalSessions().toMutableList()
        sessions.add(session)
        // 최신 순으로 정렬
        sessions.sortByDescending { it.startTime?.time ?: 0L }
        val json = gson.toJson(sessions)
        sharedPrefs.edit().putString(LOCAL_SESSIONS_KEY, json).apply()
    }
    
    private fun getLocalSessions(): List<TimerSession> {
        val json = sharedPrefs.getString(LOCAL_SESSIONS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<TimerSession>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun deleteSessionLocally(sessionId: String) {
        val sessions = getLocalSessions().toMutableList()
        sessions.removeAll { it.id == sessionId }
        val json = gson.toJson(sessions)
        sharedPrefs.edit().putString(LOCAL_SESSIONS_KEY, json).apply()
    }
    
    private fun generateLocalId(): String {
        return "local_${System.currentTimeMillis()}"
    }
}