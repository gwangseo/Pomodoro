package com.app.pomodoro.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.pomodoro.R
import com.app.pomodoro.data.model.SessionType
import com.app.pomodoro.data.model.TimerSession
import com.app.pomodoro.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 세션 기록 RecyclerView Adapter
 */
class SessionAdapter : ListAdapter<TimerSession, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class SessionViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        fun bind(session: TimerSession) {
            // 세션 시간 표시
            binding.tvDuration.text = "${session.duration}분"
            
            // 세션 타입 표시
            binding.tvSessionType.text = when (session.sessionType) {
                SessionType.WORK -> "작업"
                SessionType.BREAK -> "휴식"
            }
            
            // 시작 시간 표시
            binding.tvDateTime.text = session.startTime?.let { 
                dateFormat.format(it) 
            } ?: "시간 정보 없음"
            
            // 세션 아이콘 색상 설정
            val iconColor = when (session.sessionType) {
                SessionType.WORK -> R.color.work_mode
                SessionType.BREAK -> R.color.break_mode
            }
            binding.ivSessionIcon.backgroundTintList = 
                ContextCompat.getColorStateList(binding.root.context, iconColor)
            
            // 완료 상태 표시
            if (session.isCompleted) {
                binding.ivStatus.setImageResource(R.drawable.ic_check)
                binding.ivStatus.imageTintList = 
                    ContextCompat.getColorStateList(binding.root.context, R.color.success)
                binding.tvStatus.text = binding.root.context.getString(R.string.session_completed)
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.success)
                )
            } else {
                binding.ivStatus.setImageResource(R.drawable.ic_close)
                binding.ivStatus.imageTintList = 
                    ContextCompat.getColorStateList(binding.root.context, R.color.error)
                binding.tvStatus.text = binding.root.context.getString(R.string.session_cancelled)
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.error)
                )
            }
        }
    }
    
    private class SessionDiffCallback : DiffUtil.ItemCallback<TimerSession>() {
        override fun areItemsTheSame(oldItem: TimerSession, newItem: TimerSession): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TimerSession, newItem: TimerSession): Boolean {
            return oldItem == newItem
        }
    }
}