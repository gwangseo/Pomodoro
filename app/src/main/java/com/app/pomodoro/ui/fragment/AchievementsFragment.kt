package com.app.pomodoro.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.pomodoro.R
import com.app.pomodoro.data.model.TimerSession
import com.app.pomodoro.data.repository.SessionRepository
import com.app.pomodoro.databinding.FragmentAchievementsBinding
import com.app.pomodoro.ui.adapter.SessionAdapter
import com.app.pomodoro.ui.viewmodel.TimerViewModel
import kotlinx.coroutines.launch

/**
 * 성과 화면 Fragment
 * 뽀모도로 세션 기록과 통계를 표시
 */
class AchievementsFragment : BaseFragment() {
    
    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
    
    private val timerViewModel: TimerViewModel by activityViewModels()
    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionAdapter: SessionAdapter
    
    // 필터 상태
    private enum class FilterType { ALL, COMPLETED, CANCELLED }
    private var currentFilter = FilterType.ALL
    
    // 세션 데이터
    private var allSessions: List<TimerSession> = emptyList()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionRepository = SessionRepository(requireContext())
        setupRecyclerView()
        setupClickListeners()
        loadData()

        // 하단 네비게이션 설정 추가
        setupBottomNavigation(binding.bottomNavigation.root)
        // navigateToFragment(binding.bottomNavigation)
    }
    
    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter()
        
        // 세션 수정/삭제 콜백 설정
        sessionAdapter.onSessionEdit = { updatedSession ->
            updateSession(updatedSession)
        }
        
        sessionAdapter.onSessionDelete = { session ->
            deleteSession(session)
        }
        
        binding.rvSessions.apply {
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupClickListeners() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // 필터 버튼들
        binding.btnFilterAll.setOnClickListener {
            setFilter(FilterType.ALL)
        }
        
        binding.btnFilterCompleted.setOnClickListener {
            setFilter(FilterType.COMPLETED)
        }
        
        binding.btnFilterCancelled.setOnClickListener {
            setFilter(FilterType.CANCELLED)
        }
    }
    
    private fun loadData() {
        lifecycleScope.launch {
            try {
                // 모든 세션 로드
                val result = sessionRepository.getAllSessions()
                if (result.isSuccess) {
                    allSessions = result.getOrThrow()
                    updateUI()
                } else {
                    // 에러 처리
                    showEmptyState()
                }
                
                // 통계 데이터 로드
                loadStats()
            } catch (e: Exception) {
                showEmptyState()
            }
        }
    }
    
    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val statsResult = sessionRepository.getSessionStats()
                if (statsResult.isSuccess) {
                    val stats = statsResult.getOrThrow()
                    
                    binding.tvTotalSessions.text = stats.totalSessions.toString()
                    binding.tvCompletedSessions.text = stats.completedSessions.toString()
                    binding.tvCompletionRate.text = "${stats.completionRate.toInt()}%"
                }
            } catch (e: Exception) {
                // 통계 로드 실패 시 기본값 유지
            }
        }
    }
    
    private fun setFilter(filterType: FilterType) {
        currentFilter = filterType
        updateFilterButtons()
        updateSessionList()
    }
    
    private fun updateFilterButtons() {
        // 모든 버튼을 기본 스타일로 초기화
        binding.btnFilterAll.apply {
            setBackgroundColor(requireContext().getColor(R.color.surface_dark))
            setTextColor(requireContext().getColor(R.color.button_text))
            setStrokeColorResource(R.color.primary_blue)
        }
        
        binding.btnFilterCompleted.apply {
            setBackgroundColor(requireContext().getColor(R.color.surface_dark))
            setTextColor(requireContext().getColor(R.color.button_text))
            setStrokeColorResource(R.color.success)
        }
        
        binding.btnFilterCancelled.apply {
            setBackgroundColor(requireContext().getColor(R.color.surface_dark))
            setTextColor(requireContext().getColor(R.color.button_text))
            setStrokeColorResource(R.color.error)
        }
        
        // 선택된 버튼 강조
        when (currentFilter) {
            FilterType.ALL -> {
                binding.btnFilterAll.setBackgroundColor(
                    requireContext().getColor(R.color.primary_blue)
                )
                binding.btnFilterAll.setTextColor(requireContext().getColor(R.color.button_text))
            }
            FilterType.COMPLETED -> {
                binding.btnFilterCompleted.setBackgroundColor(
                    requireContext().getColor(R.color.success)
                )
                binding.btnFilterCompleted.setTextColor(requireContext().getColor(R.color.button_text))
            }
            FilterType.CANCELLED -> {
                binding.btnFilterCancelled.setBackgroundColor(
                    requireContext().getColor(R.color.error)
                )
                binding.btnFilterCancelled.setTextColor(requireContext().getColor(R.color.button_text))
            }
        }
    }
    
    private fun updateSessionList() {
        val filteredSessions = when (currentFilter) {
            FilterType.ALL -> allSessions
            FilterType.COMPLETED -> allSessions.filter { it.isCompleted }
            FilterType.CANCELLED -> allSessions.filter { !it.isCompleted }
        }
        
        sessionAdapter.submitList(filteredSessions)
        
        // 빈 상태 처리
        if (filteredSessions.isEmpty()) {
            showEmptyState()
        } else {
            showSessionList()
        }
    }
    
    private fun updateUI() {
        updateFilterButtons()
        updateSessionList()
    }
    
    private fun showEmptyState() {
        binding.rvSessions.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
    }
    
    private fun showSessionList() {
        binding.rvSessions.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
    }
    
    /**
     * 세션 수정
     */
    private fun updateSession(updatedSession: TimerSession) {
        lifecycleScope.launch {
            try {
                val userId = sessionRepository.getUserId()
                val result = sessionRepository.updateSession(updatedSession, userId)
                if (result.isSuccess) {
                    // 데이터 다시 로드
                    loadData()
                } else {
                    // 에러 처리
                    showError("세션 수정에 실패했습니다.")
                }
            } catch (e: Exception) {
                showError("세션 수정 중 오류가 발생했습니다.")
            }
        }
    }
    
    /**
     * 세션 삭제
     */
    private fun deleteSession(session: TimerSession) {
        lifecycleScope.launch {
            try {
                val userId = sessionRepository.getUserId()
                val result = sessionRepository.deleteSession(session.id, userId)
                if (result.isSuccess) {
                    // 데이터 다시 로드
                    loadData()
                } else {
                    // 에러 처리
                    showError("세션 삭제에 실패했습니다.")
                }
            } catch (e: Exception) {
                showError("세션 삭제 중 오류가 발생했습니다.")
            }
        }
    }
    
    /**
     * 에러 메시지 표시
     */
    private fun showError(message: String) {
        // 간단한 토스트 메시지로 표시
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}