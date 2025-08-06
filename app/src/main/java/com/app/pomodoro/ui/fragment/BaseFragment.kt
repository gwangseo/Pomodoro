package com.app.pomodoro.ui.fragment

import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.app.pomodoro.R
import com.app.pomodoro.databinding.BottomNavigationBinding

abstract class BaseFragment : Fragment() {

    /**
     * 하단 네비게이션 바 설정
     */
    protected fun setupBottomNavigation(bottomNav: LinearLayout) {
        bottomNav.findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            navigateToFragment(R.id.homeFragment)
        }

        bottomNav.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            navigateToFragment(R.id.settingsFragment)
        }

        bottomNav.findViewById<ImageButton>(R.id.btnAchievements).setOnClickListener {
            navigateToFragment(R.id.achievementsFragment)
        }

        // 현재 화면에 따라 버튼 상태 변경
        updateNavButtonState(bottomNav)
    }

    /**
     * 안전한 네비게이션 처리
     */
    private fun navigateToFragment(destinationId: Int) {
        try {
            val currentDestination = findNavController().currentDestination?.id
            if (currentDestination != destinationId) {
                when (destinationId) {
                    R.id.homeFragment -> {
                        if (currentDestination == R.id.settingsFragment) {
                            findNavController().navigate(R.id.action_settings_to_home)
                        } else if (currentDestination == R.id.achievementsFragment) {
                            findNavController().navigate(R.id.action_achievements_to_home)
                        }
                    }
                    R.id.settingsFragment -> {
                        if (currentDestination == R.id.homeFragment) {
                            findNavController().navigate(R.id.action_home_to_settings)
                        } else if (currentDestination == R.id.achievementsFragment) {
                            findNavController().navigate(R.id.action_achievements_to_settings)
                        }
                    }
                    R.id.achievementsFragment -> {
                        if (currentDestination == R.id.homeFragment) {
                            findNavController().navigate(R.id.action_home_to_achievements)
                        } else if (currentDestination == R.id.settingsFragment) {
                            findNavController().navigate(R.id.action_settings_to_achievements)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 네비게이션 에러 처리
        }
    }

    /**
     * 현재 화면에 따라 네비게이션 버튼 상태 업데이트
     */
    private fun updateNavButtonState(bottomNav: LinearLayout) {
        val currentDestination = findNavController().currentDestination?.id

        val homeBtn = bottomNav.findViewById<ImageButton>(R.id.btnHome)
        val settingsBtn = bottomNav.findViewById<ImageButton>(R.id.btnSettings)
        val achievementsBtn = bottomNav.findViewById<ImageButton>(R.id.btnAchievements)

        // 모든 버튼을 기본 상태로 초기화
        homeBtn.alpha = 0.6f
        settingsBtn.alpha = 0.6f
        achievementsBtn.alpha = 0.6f

        // 현재 화면 버튼을 활성화 상태로 변경
        when (currentDestination) {
            R.id.homeFragment -> homeBtn.alpha = 1.0f
            R.id.settingsFragment -> settingsBtn.alpha = 1.0f
            R.id.achievementsFragment -> achievementsBtn.alpha = 1.0f
        }
    }
}