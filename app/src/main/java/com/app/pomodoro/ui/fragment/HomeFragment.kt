package com.app.pomodoro.ui.fragment

import android.R.attr.backgroundTint
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
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
class HomeFragment : BaseFragment() {
    
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
        setupBottomNavigation(binding.root.findViewById(R.id.bottomNavigation))
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
            timerViewModel.cancelCurrentSession()
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
        
        // 시간 설정 버튼
        binding.btnTimeSetting.setOnClickListener {
            showTimePickerDialog()
        }
        
        // 집중/휴식 선택 버튼
        binding.btnWork.setOnClickListener {
            timerViewModel.setSessionType(SessionType.WORK)
        }
        
        binding.btnBreak.setOnClickListener {
            timerViewModel.setSessionType(SessionType.BREAK)
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
    
    @SuppressLint("ResourceAsColor")
    private fun updateSessionTypeUI(sessionType: SessionType) {
        when (sessionType) {
            SessionType.WORK -> {
                binding.circularTimer.setWorkMode(true)
                
                // 집중 버튼 활성화, 휴식 버튼 비활성화
                binding.btnWork.apply {
                    setBackgroundColor(requireContext().getColor(R.color.primary_blue))
                    setTextColor(requireContext().getColor(android.R.color.white))
                }
                binding.btnBreak.apply {
                    setBackgroundColor(android.R.color.transparent)
                    setTextColor(requireContext().getColor(R.color.break_mode))
                    strokeColor = requireContext().getColorStateList(R.color.break_mode)
                    setStrokeColorResource(R.color.break_mode)
                }
            }
            SessionType.BREAK -> {
                binding.circularTimer.setWorkMode(false)
                
                // 휴식 버튼 활성화, 집중 버튼 비활성화
                binding.btnBreak.apply {
                    setBackgroundColor(requireContext().getColor(R.color.break_mode))
                    setTextColor(requireContext().getColor(android.R.color.white))
                }
                binding.btnWork.apply {
                    setBackgroundColor(android.R.color.transparent)
                    setTextColor(requireContext().getColor(R.color.primary_blue))
                    strokeColor = requireContext().getColorStateList(R.color.primary_blue)
                    setStrokeColorResource(R.color.primary_blue)
                }
            }
        }
    }

    private fun showTimePickerDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_time_picker, null)
        val numberPicker = dialogView.findViewById<com.app.pomodoro.ui.view.HorizontalNumberPicker>(R.id.numberPicker)

        numberPicker.setMinValue(0)
        numberPicker.setMaxValue(60)
        numberPicker.setValue((timerViewModel.currentTime.value ?: 1500) / 60)

        val dialog = AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle("타이머 시간 설정")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                timerViewModel.setCustomTime(numberPicker.getValue())
            }
            .setNegativeButton("취소", null)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(R.color.background_dark)
        dialog.show()
    }
    
    /*
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
    */

    
    private fun showTimerCompletedDialog() {
        val sessionType = timerViewModel.sessionType.value ?: SessionType.WORK
        val message = if (sessionType == SessionType.WORK) {
            getString(R.string.notification_work_completed)
        } else {
            getString(R.string.notification_break_completed)
        }
        
        val completedDialog = AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
            .setTitle(getString(R.string.timer_completed))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .create()
        
        completedDialog.window?.setBackgroundDrawableResource(R.color.background_dark)
        completedDialog.show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}