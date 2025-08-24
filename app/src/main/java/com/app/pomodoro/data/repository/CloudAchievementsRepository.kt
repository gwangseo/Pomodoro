package com.app.pomodoro.data.repository

import android.util.Log
import com.app.pomodoro.data.model.TimerSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Firebase Firestore를 사용하여 Achievements를 클라우드에 저장하는 Repository
 * 웹 버전과 연동 가능
 */
class CloudAchievementsRepository {
    
    private val db = FirebaseFirestore.getInstance()
    private val sessionsCollection = "sessions"
    private val usersCollection = "users"
    
    /**
     * 세션을 클라우드에 저장
     */
    suspend fun saveSession(userId: String, session: TimerSession): Result<Unit> {
        return try {
            Log.d("CloudRepo", "세션 저장 시작: userId=$userId, sessionId=${session.id}")
            
            val sessionData = hashMapOf(
                "id" to session.id,
                "userId" to userId,
                "startTime" to session.startTime,
                "endTime" to session.endTime,
                "duration" to session.duration,
                "actualDuration" to session.actualDuration,
                "sessionType" to session.sessionType.name,
                "isCompleted" to session.isCompleted
            )
            
            Log.d("CloudRepo", "저장할 데이터: $sessionData")
            
            db.collection(sessionsCollection)
                .document(session.id)
                .set(sessionData)
                .await()
            
            Log.d("CloudRepo", "세션 저장 성공: ${session.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CloudRepo", "세션 저장 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 사용자의 모든 세션 가져오기
     */
    suspend fun getUserSessions(userId: String): Result<List<TimerSession>> {
        return try {
            Log.d("CloudRepo", "사용자 세션 조회 시작: userId=$userId")
            
            val snapshot = db.collection(sessionsCollection)
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Log.d("CloudRepo", "조회된 세션 수: ${snapshot.size()}")
            
            val sessions = snapshot.documents.mapNotNull { doc ->
                try {
                    TimerSession(
                        id = doc.getString("id") ?: return@mapNotNull null,
                        startTime = doc.getDate("startTime"),
                        endTime = doc.getDate("endTime"),
                        duration = doc.getLong("duration")?.toInt() ?: 0,
                        actualDuration = doc.getLong("actualDuration")?.toInt() ?: 0,
                        sessionType = com.app.pomodoro.data.model.SessionType.valueOf(
                            doc.getString("sessionType") ?: "WORK"
                        ),
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        userId = doc.getString("userId")
                    )
                } catch (e: Exception) {
                    Log.e("CloudRepo", "세션 파싱 실패: ${e.message}", e)
                    null
                }
            }
            
            Log.d("CloudRepo", "파싱된 세션 수: ${sessions.size}")
            Result.success(sessions)
        } catch (e: Exception) {
            Log.e("CloudRepo", "사용자 세션 조회 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 사용자 통계 가져오기
     */
    suspend fun getUserStats(userId: String): Result<Map<String, Any>> {
        return try {
            Log.d("CloudRepo", "사용자 통계 조회 시작: userId=$userId")
            
            val snapshot = db.collection(sessionsCollection)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val totalSessions = snapshot.size()
            val completedSessions = snapshot.documents.count { 
                it.getBoolean("isCompleted") == true 
            }
            val totalDuration = snapshot.documents.sumOf { 
                it.getLong("actualDuration")?.toLong() ?: 0L 
            }
            
            val stats = mapOf(
                "totalSessions" to totalSessions,
                "completedSessions" to completedSessions,
                "failedSessions" to (totalSessions - completedSessions),
                "totalDuration" to totalDuration
            )
            
            Log.d("CloudRepo", "통계 결과: $stats")
            Result.success(stats)
        } catch (e: Exception) {
            Log.e("CloudRepo", "사용자 통계 조회 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 사용자 정보 저장
     */
    suspend fun saveUserInfo(userId: String, email: String, name: String): Result<Unit> {
        return try {
            Log.d("CloudRepo", "사용자 정보 저장 시작: userId=$userId, email=$email")
            
            val userData = hashMapOf(
                "email" to email,
                "name" to name,
                "lastLogin" to Date()
            )
            
            db.collection(usersCollection)
                .document(userId)
                .set(userData)
                .await()
            
            Log.d("CloudRepo", "사용자 정보 저장 성공: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CloudRepo", "사용자 정보 저장 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 세션 삭제
     */
    suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            Log.d("CloudRepo", "세션 삭제 시작: sessionId=$sessionId")
            
            db.collection(sessionsCollection)
                .document(sessionId)
                .delete()
                .await()
            
            Log.d("CloudRepo", "세션 삭제 성공: $sessionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CloudRepo", "세션 삭제 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 세션 수정
     */
    suspend fun updateSession(userId: String, updatedSession: TimerSession): Result<Unit> {
        return try {
            Log.d("CloudRepo", "세션 수정 시작: userId=$userId, sessionId=${updatedSession.id}")
            
            val sessionData = hashMapOf(
                "id" to updatedSession.id,
                "userId" to userId,
                "startTime" to updatedSession.startTime,
                "endTime" to updatedSession.endTime,
                "duration" to updatedSession.duration,
                "actualDuration" to updatedSession.actualDuration,
                "sessionType" to updatedSession.sessionType.name,
                "isCompleted" to updatedSession.isCompleted
            )
            
            Log.d("CloudRepo", "수정할 데이터: $sessionData")
            
            db.collection(sessionsCollection)
                .document(updatedSession.id)
                .set(sessionData)
                .await()
            
            Log.d("CloudRepo", "세션 수정 성공: ${updatedSession.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CloudRepo", "세션 수정 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 사용자의 모든 세션 삭제
     */
    suspend fun deleteAllUserSessions(userId: String): Result<Unit> {
        return try {
            Log.d("CloudRepo", "사용자 모든 세션 삭제 시작: userId=$userId")
            
            val snapshot = db.collection(sessionsCollection)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Log.d("CloudRepo", "사용자 모든 세션 삭제 성공: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CloudRepo", "사용자 모든 세션 삭제 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
}
