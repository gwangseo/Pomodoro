package com.app.pomodoro.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.pomodoro.databinding.FragmentSettingsBinding
import com.app.pomodoro.ui.viewmodel.AuthState
import com.app.pomodoro.ui.viewmodel.AuthViewModel
import com.app.pomodoro.ui.viewmodel.TimerViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

/**
 * 설정 화면 Fragment
 * Google 로그인, 타이머 설정 등을 관리
 */
class SettingsFragment : BaseFragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val timerViewModel: TimerViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    
    // Google 로그인 결과 처리
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("SettingsFragment", "Google 로그인 결과: resultCode=${result.resultCode}")
        android.util.Log.d("SettingsFragment", "Google 로그인 결과: data=${result.data}")
        
        // Intent의 extras 확인
        result.data?.let { intent ->
            android.util.Log.d("SettingsFragment", "Intent extras: ${intent.extras}")
            intent.extras?.keySet()?.forEach { key ->
                android.util.Log.d("SettingsFragment", "Extra key: $key, value: ${intent.extras?.get(key)}")
            }
        }
        
        if (result.resultCode == Activity.RESULT_OK) {
            android.util.Log.d("SettingsFragment", "Google 로그인 성공, 계정 정보 추출 중...")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                android.util.Log.d("SettingsFragment", "Google 계정 정보: ${account.email}")
                authViewModel.signInWithGoogle(account)
            } catch (e: ApiException) {
                android.util.Log.e("SettingsFragment", "Google 로그인 실패: ${e.message}", e)
                android.util.Log.e("SettingsFragment", "Google 로그인 실패 코드: ${e.statusCode}")
                Toast.makeText(requireContext(), "Google 로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            android.util.Log.e("SettingsFragment", "Google 로그인 취소 또는 실패: resultCode=${result.resultCode}")
            android.util.Log.e("SettingsFragment", "Google 로그인 취소 또는 실패: data=${result.data}")
            
            // resultCode에 따른 구체적인 메시지
            val message = when (result.resultCode) {
                Activity.RESULT_CANCELED -> "사용자가 로그인을 취소했습니다."
                else -> "로그인 실패 (코드: ${result.resultCode})"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()
        updateUI()

        // 하단 네비게이션 설정
        setupBottomNavigation(binding.bottomNavigation.root)
    }
    
    private fun setupClickListeners() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Google 로그인 버튼
        binding.btnGoogleLogin.setOnClickListener {
            android.util.Log.d("SettingsFragment", "Google 로그인 버튼 클릭됨")
            val signInIntent = authViewModel.getGoogleSignInClient().signInIntent
            android.util.Log.d("SettingsFragment", "Google 로그인 인텐트 생성됨")
            googleSignInLauncher.launch(signInIntent)
        }
        
        // 로그아웃 버튼
        binding.btnLogout.setOnClickListener {
            authViewModel.signOut()
        }
    }
    
    private fun setupObservers() {
        // 인증 상태 관찰
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.LOADING -> {
                    // 로딩 상태 표시
                    binding.btnGoogleLogin.isEnabled = false
                    binding.btnLogout.isEnabled = false
                }
                is AuthState.SIGNED_IN -> {
                    // 로그인 상태 표시
                    showLoginSuccess()
                    binding.btnGoogleLogin.isEnabled = false
                    binding.btnLogout.isEnabled = true
                }
                is AuthState.SIGNED_OUT -> {
                    // 로그아웃 상태 표시
                    showLoginBefore()
                    binding.btnGoogleLogin.isEnabled = true
                    binding.btnLogout.isEnabled = false
                }
                is AuthState.ERROR -> {
                    // 에러 상태 표시
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    binding.btnGoogleLogin.isEnabled = true
                    binding.btnLogout.isEnabled = false
                }
            }
        }
        
        // 현재 사용자 정보 관찰
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvUserName.text = user.displayName ?: "사용자"
                binding.tvUserEmail.text = user.email ?: "user@example.com"
            }
        }
    }
    
    private fun updateUI() {
        // 초기 상태는 로그인 전으로 설정
        showLoginBefore()
    }
    
    /**
     * 로그인 전 UI 표시
     */
    private fun showLoginBefore() {
        binding.layoutLoginBefore.visibility = View.VISIBLE
        binding.layoutLoginAfter.visibility = View.GONE
    }
    
    /**
     * 로그인 후 UI 표시
     */
    private fun showLoginSuccess() {
        binding.layoutLoginBefore.visibility = View.GONE
        binding.layoutLoginAfter.visibility = View.VISIBLE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}