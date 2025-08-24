package com.app.pomodoro.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.app.pomodoro.data.model.TimerSession
import com.app.pomodoro.data.model.SessionStats
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 세션 데이터를 관리하는 Repository
 * 로컬 SharedPreferences와 Firebase Firestore 동기화
 */
class SessionRepository(private val context: Context) {
    
    private val sharedPrefs = context.getSharedPreferences("pomodoro_sessions", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cloudRepository = CloudAchievementsRepository()
    
    companion object {
        private const val LOCAL_SESSIONS_KEY = "local_sessions"
        private const val USER_ID_KEY = "current_user_id"
    }
    
    /**
     * 새로운 세션 저장 (로컬 + 클라우드)
     */
    suspend fun saveSession(session: TimerSession, userId: String? = null): Result<String> {
        return try {
            Log.d("SessionRepo", "세션 저장 시작: sessionId=${session.id}, userId=$userId")
            
            val sessionWithId = session.copy(id = generateLocalId())
            
            // 로컬에 저장
            saveSessionLocally(sessionWithId)
            Log.d("SessionRepo", "로컬 저장 완료: ${sessionWithId.id}")
            
            // 클라우드에 저장 (사용자가 로그인된 경우)
            userId?.let { uid ->
                Log.d("SessionRepo", "클라우드 저장 시작: userId=$uid")
                val cloudResult = cloudRepository.saveSession(uid, sessionWithId)
                if (cloudResult.isSuccess) {
                    Log.d("SessionRepo", "클라우드 저장 성공: ${sessionWithId.id}")
                } else {
                    Log.e("SessionRepo", "클라우드 저장 실패: ${cloudResult.exceptionOrNull()?.message}")
                }
            }
            
            Result.success(sessionWithId.id)
        } catch (e: Exception) {
            Log.e("SessionRepo", "세션 저장 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 사용자의 모든 세션 조회 (로컬 + 클라우드)
     */
    suspend fun getAllSessions(userId: String? = null): Result<List<TimerSession>> {
        return try {
            Log.d("SessionRepo", "세션 조회 시작: userId=$userId")
            
            val localSessions = getLocalSessions()
            Log.d("SessionRepo", "로컬 세션 수: ${localSessions.size}")
            
            // 클라우드에서 세션 가져오기 (사용자가 로그인된 경우)
            val cloudSessions = if (userId != null) {
                Log.d("SessionRepo", "클라우드 세션 조회 시작")
                val cloudResult = cloudRepository.getUserSessions(userId)
                if (cloudResult.isSuccess) {
                    val sessions = cloudResult.getOrNull() ?: emptyList()
                    Log.d("SessionRepo", "클라우드 세션 수: ${sessions.size}")
                    sessions
                } else {
                    Log.e("SessionRepo", "클라우드 세션 조회 실패: ${cloudResult.exceptionOrNull()?.message}")
                    emptyList()
                }
            } else {
                emptyList()
            }
            
            // 로컬과 클라우드 세션 병합 (중복 제거)
            val mergedSessions = (localSessions + cloudSessions)
                .distinctBy { it.id }
                .sortedByDescending { it.startTime }
            
            Log.d("SessionRepo", "병합된 세션 수: ${mergedSessions.size}")
            Result.success(mergedSessions)
        } catch (e: Exception) {
            Log.e("SessionRepo", "세션 조회 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 완료된 세션 조회
     */
    suspend fun getCompletedSessions(userId: String? = null): Result<List<TimerSession>> {
        return try {
            val allSessions = getAllSessions(userId).getOrNull() ?: emptyList()
            val completedSessions = allSessions.filter { it.isCompleted }
            Log.d("SessionRepo", "완료된 세션 수: ${completedSessions.size}")
            Result.success(completedSessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 취소된 세션 조회
     */
    suspend fun getCancelledSessions(userId: String? = null): Result<List<TimerSession>> {
        return try {
            val allSessions = getAllSessions(userId).getOrNull() ?: emptyList()
            val cancelledSessions = allSessions.filter { !it.isCompleted }
            Log.d("SessionRepo", "취소된 세션 수: ${cancelledSessions.size}")
            Result.success(cancelledSessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 세션 통계 조회
     */
    suspend fun getSessionStats(userId: String? = null): Result<SessionStats> {
        return try {
            val allSessions = getAllSessions(userId).getOrNull() ?: emptyList()
            val totalSessions = allSessions.size
            val completedSessions = allSessions.count { it.isCompleted }
            val totalWorkTime = allSessions.sumOf { it.actualDuration }
            val averageSessionLength = if (totalSessions > 0) totalWorkTime.toDouble() / totalSessions else 0.0
            val completionRate = if (totalSessions > 0) (completedSessions.toDouble() / totalSessions) * 100 else 0.0
            
            val stats = SessionStats(
                totalSessions = totalSessions,
                completedSessions = completedSessions,
                totalWorkTime = totalWorkTime,
                averageSessionLength = averageSessionLength,
                completionRate = completionRate
            )
            
            Log.d("SessionRepo", "통계 계산 완료: $stats")
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 세션 삭제 (로컬 + 클라우드)
     */
    suspend fun deleteSession(sessionId: String, userId: String? = null): Result<Unit> {
        return try {
            Log.d("SessionRepo", "세션 삭제 시작: sessionId=$sessionId, userId=$userId")
            
            // 로컬에서 삭제
            deleteSessionLocally(sessionId)
            Log.d("SessionRepo", "로컬 삭제 완료: $sessionId")
            
            // 클라우드에서 삭제 (사용자가 로그인된 경우)
            userId?.let { uid ->
                val cloudResult = cloudRepository.deleteSession(sessionId)
                if (cloudResult.isSuccess) {
                    Log.d("SessionRepo", "클라우드 삭제 성공: $sessionId")
                } else {
                    Log.e("SessionRepo", "클라우드 삭제 실패: ${cloudResult.exceptionOrNull()?.message}")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 세션 수정 (로컬 + 클라우드)
     */
    suspend fun updateSession(updatedSession: TimerSession, userId: String? = null): Result<Unit> {
        return try {
            Log.d("SessionRepo", "세션 수정 시작: sessionId=${updatedSession.id}, userId=$userId")
            
            // 로컬에서 수정
            updateSessionLocally(updatedSession)
            Log.d("SessionRepo", "로컬 수정 완료: ${updatedSession.id}")
            
            // 클라우드에서 수정 (사용자가 로그인된 경우)
            userId?.let { uid ->
                val cloudResult = cloudRepository.updateSession(uid, updatedSession)
                if (cloudResult.isSuccess) {
                    Log.d("SessionRepo", "클라우드 수정 성공: ${updatedSession.id}")
                } else {
                    Log.e("SessionRepo", "클라우드 수정 실패: ${cloudResult.exceptionOrNull()?.message}")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SessionRepo", "세션 수정 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 사용자 ID 저장
     */
    fun saveUserId(userId: String) {
        Log.d("SessionRepo", "사용자 ID 저장: $userId")
        sharedPrefs.edit().putString(USER_ID_KEY, userId).apply()
    }
    
    /**
     * 저장된 사용자 ID 가져오기
     */
    fun getUserId(): String? {
        val userId = sharedPrefs.getString(USER_ID_KEY, null)
        Log.d("SessionRepo", "저장된 사용자 ID: $userId")
        return userId
    }
    
    /**
     * 사용자 ID 삭제 (로그아웃 시)
     */
    fun clearUserId() {
        Log.d("SessionRepo", "사용자 ID 삭제")
        sharedPrefs.edit().remove(USER_ID_KEY).apply()
    }
    
    /**
     * 클라우드에서 사용자 통계 가져오기
     */
    suspend fun getCloudStats(userId: String): Result<Map<String, Any>> {
        return try {
            Log.d("SessionRepo", "클라우드 통계 조회 시작: userId=$userId")
            val result = cloudRepository.getUserStats(userId)
            if (result.isSuccess) {
                Log.d("SessionRepo", "클라우드 통계 조회 성공: ${result.getOrNull()}")
            } else {
                Log.e("SessionRepo", "클라우드 통계 조회 실패: ${result.exceptionOrNull()?.message}")
            }
            result
        } catch (e: Exception) {
            Log.e("SessionRepo", "클라우드 통계 조회 실패: ${e.message}", e)
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

    private fun updateSessionLocally(updatedSession: TimerSession) {
        val sessions = getLocalSessions().toMutableList()
        sessions.removeAll { it.id == updatedSession.id }
        sessions.add(updatedSession)
        sessions.sortByDescending { it.startTime?.time ?: 0L }
        val json = gson.toJson(sessions)
        sharedPrefs.edit().putString(LOCAL_SESSIONS_KEY, json).apply()
    }
    
    private fun generateLocalId(): String {
        return "local_${System.currentTimeMillis()}"
    }
}