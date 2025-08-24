package com.app.pomodoro.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication을 관리하는 Repository
 */
class AuthRepository(private val context: Context) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient
    
    init {
        setupGoogleSignIn()
    }
    
    private fun setupGoogleSignIn() {
        android.util.Log.d("AuthRepository", "Google 로그인 설정 시작")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("744666566636-2uc9ahisd90c7jqi8207m16ole1u8jst.apps.googleusercontent.com") // google-services.json에서 가져온 Web Client ID
            .requestEmail()
            .build()
        
        android.util.Log.d("AuthRepository", "GoogleSignInOptions 생성 완료")
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        android.util.Log.d("AuthRepository", "GoogleSignInClient 생성 완료")
    }
    
    /**
     * 현재 로그인된 사용자 반환
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    /**
     * Google 로그인 클라이언트 반환
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        return googleSignInClient
    }
    
    /**
     * Google 계정으로 Firebase 인증
     */
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            android.util.Log.d("AuthRepository", "Firebase 인증 시작: ${account.email}")
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            android.util.Log.d("AuthRepository", "GoogleAuthProvider 크레덴셜 생성 완료")
            val result = auth.signInWithCredential(credential).await()
            android.util.Log.d("AuthRepository", "Firebase 인증 성공: ${result.user?.email}")
            Result.success(result.user!!)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Firebase 인증 실패: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 로그아웃
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            googleSignInClient.signOut().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 사용자 ID 반환
     */
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * 사용자 이메일 반환
     */
    fun getUserEmail(): String? {
        return auth.currentUser?.email
    }
    
    /**
     * 사용자 이름 반환
     */
    fun getUserName(): String? {
        return auth.currentUser?.displayName
    }
}
