package com.app.pomodoro.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.pomodoro.R
import com.app.pomodoro.databinding.FragmentSettingsBinding
import com.app.pomodoro.ui.viewmodel.TimerViewModel

/**
 * 설정 화면 Fragment
 * Google 로그인, 타이머 설정 등을 관리
 */
class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val timerViewModel: TimerViewModel by activityViewModels()
    
    // 현재 선택된 타이머 시간 (분)
    private var selectedDuration = 25
    
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
    }
    
    private fun setupClickListeners() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Google 로그인 버튼
        binding.btnGoogleLogin.setOnClickListener {
            // TODO: Google 로그인 구현
            showLoginSuccess()
        }
        
        // 로그아웃 버튼
        binding.btnLogout.setOnClickListener {
            // TODO: 로그아웃 구현
            showLoginBefore()
        }
        
        // 타이머 시간 설정 버튼들
        binding.btn15Min.setOnClickListener {
            setTimerDuration(15)
        }
        
        binding.btn25Min.setOnClickListener {
            setTimerDuration(25)
        }
        
        binding.btn30Min.setOnClickListener {
            setTimerDuration(30)
        }
        
        binding.btn50Min.setOnClickListener {
            setTimerDuration(50)
        }
    }
    
    private fun setupObservers() {
        // 타이머 설정 관찰
        timerViewModel.timerSettings.observe(viewLifecycleOwner) { settings ->
            selectedDuration = settings.workDuration
            updateTimerDurationButtons()
        }
    }
    
    private fun updateUI() {
        // 초기 상태는 로그인 전으로 설정
        showLoginBefore()
        updateTimerDurationButtons()
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
        
        // 임시 사용자 정보 표시 (실제로는 Firebase에서 가져와야 함)
        binding.tvUserName.text = "사용자"
        binding.tvUserEmail.text = "user@example.com"
    }
    
    /**
     * 타이머 시간 설정
     */
    private fun setTimerDuration(minutes: Int) {
        selectedDuration = minutes
        updateTimerDurationButtons()
        
        // ViewModel에 새로운 시간 설정 반영
        timerViewModel.setCustomTime(minutes)
    }
    
    /**
     * 타이머 시간 버튼 UI 업데이트
     */
    private fun updateTimerDurationButtons() {
        // 모든 버튼을 기본 스타일로 초기화
        val buttons = listOf(
            binding.btn15Min to 15,
            binding.btn25Min to 25,
            binding.btn30Min to 30,
            binding.btn50Min to 50
        )
        
        buttons.forEach { (button, duration) ->
            if (duration == selectedDuration) {
                // 선택된 버튼 스타일
                button.setBackgroundColor(requireContext().getColor(R.color.primary_mint_blue))
                button.setTextColor(requireContext().getColor(R.color.white))
            } else {
                // 기본 버튼 스타일
                button.setBackgroundColor(requireContext().getColor(R.color.white))
                button.setTextColor(requireContext().getColor(R.color.primary_mint_blue))
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}