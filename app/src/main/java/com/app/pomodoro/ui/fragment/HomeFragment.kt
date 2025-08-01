package com.app.pomodoro.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.pomodoro.R
import com.app.pomodoro.data.model.SessionType
import com.app.pomodoro.data.model.TimerState
import com.app.pomodoro.databinding.FragmentHomeBinding
import com.app.pomodoro.ui.viewmodel.TimerViewModel

/**
 * 홈 화면 Fragment
 * 뽀모도로 타이머의 메인 기능을 담당
 */
class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val timerViewModel: TimerViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        // 타이머 상태 관찰
        timerViewModel.timerState.observe(viewLifecycleOwner) { state ->
            updateTimerStateUI(state)
        }
        
        // 현재 시간 관찰
        timerViewModel.currentTime.observe(viewLifecycleOwner) { timeInSeconds ->
            val formattedTime = timerViewModel.formatTime(timeInSeconds)
            binding.tvDigitalTimer.text = formattedTime
            binding.circularTimer.updateTime(timeInSeconds)
        }
        
        // 총 시간 관찰
        timerViewModel.totalTime.observe(viewLifecycleOwner) { totalTimeInSeconds ->
            binding.circularTimer.setTotalTime(totalTimeInSeconds)
        }
        
        // 세션 타입 관찰
        timerViewModel.sessionType.observe(viewLifecycleOwner) { sessionType ->
            updateSessionTypeUI(sessionType)
        }
    }
    
    private fun setupClickListeners() {
        // 원형 타이머 클릭 - 시작/일시정지
        binding.circularTimer.setOnClickListener {
            timerViewModel.startTimer()
        }
        
        // 시작/일시정지 버튼
        binding.btnStartPause.setOnClickListener {
            timerViewModel.startTimer()
        }
        
        // 초기화 버튼
        binding.btnClear.setOnClickListener {
            showResetConfirmationDialog()
        }
        
        // 디지털 타이머 클릭 - 시간 설정
        binding.tvDigitalTimer.setOnClickListener {
            showTimePickerDialog()
        }
        
        // 설정 버튼
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
        
        // 성과 버튼
        binding.btnAchievements.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_achievements)
        }
    }
    
    private fun updateTimerStateUI(state: TimerState) {
        binding.circularTimer.setTimerState(state)
        
        when (state) {
            TimerState.IDLE -> {
                binding.btnStartPause.text = getString(R.string.start_timer)
                binding.btnStartPause.setIconResource(R.drawable.ic_play)
            }
            TimerState.RUNNING -> {
                binding.btnStartPause.text = getString(R.string.pause_timer)
                binding.btnStartPause.setIconResource(R.drawable.ic_pause)
            }
            TimerState.PAUSED -> {
                binding.btnStartPause.text = getString(R.string.start_timer)
                binding.btnStartPause.setIconResource(R.drawable.ic_play)
            }
            TimerState.COMPLETED -> {
                binding.btnStartPause.text = getString(R.string.start_timer)
                binding.btnStartPause.setIconResource(R.drawable.ic_play)
                showTimerCompletedDialog()
            }
        }
    }
    
    private fun updateSessionTypeUI(sessionType: SessionType) {
        when (sessionType) {
            SessionType.WORK -> {
                binding.tvTimerState.text = getString(R.string.work_session)
                binding.tvTimerState.setTextColor(requireContext().getColor(R.color.work_mode))
                binding.circularTimer.setWorkMode(true)
            }
            SessionType.BREAK -> {
                binding.tvTimerState.text = getString(R.string.break_session)
                binding.tvTimerState.setTextColor(requireContext().getColor(R.color.break_mode))
                binding.circularTimer.setWorkMode(false)
            }
        }
    }
    
    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("타이머 초기화")
            .setMessage("현재 진행 중인 타이머를 초기화하시겠습니까?")
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                timerViewModel.cancelCurrentSession()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showTimePickerDialog() {
        val options = arrayOf("15분", "25분", "30분", "50분")
        val durations = arrayOf(15, 25, 30, 50)
        
        AlertDialog.Builder(requireContext())
            .setTitle("타이머 시간 설정")
            .setItems(options) { _, which ->
                timerViewModel.setCustomTime(durations[which])
            }
            .show()
    }
    
    private fun showTimerCompletedDialog() {
        val sessionType = timerViewModel.sessionType.value ?: SessionType.WORK
        val message = if (sessionType == SessionType.WORK) {
            getString(R.string.notification_work_completed)
        } else {
            getString(R.string.notification_break_completed)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.timer_completed))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}