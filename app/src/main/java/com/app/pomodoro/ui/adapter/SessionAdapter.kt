package com.app.pomodoro.ui.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.NumberPicker
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
    
    // 세션 수정 콜백
    var onSessionEdit: ((TimerSession) -> Unit)? = null
    var onSessionDelete: ((TimerSession) -> Unit)? = null
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding, onSessionEdit, onSessionDelete)
    }
    
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class SessionViewHolder(
        private val binding: ItemSessionBinding,
        private val onSessionEdit: ((TimerSession) -> Unit)?,
        private val onSessionDelete: ((TimerSession) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        fun bind(session: TimerSession) {
            // 세션 시간 표시
            binding.tvDuration.text = "${session.duration}분"
            
                         // 세션 타입 표시
             binding.tvSessionType.text = when (session.sessionType) {
                 SessionType.WORK -> "집중"
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
            
                    // 아이템 클릭 시 바로 수정 다이얼로그 표시
        binding.root.setOnClickListener {
            showEditSessionDialog(session)
        }
        }
        

        
        @SuppressLint("ResourceAsColor")
        private fun showEditSessionDialog(session: TimerSession) {
            val context = binding.root.context
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.dialog_session_edit, null)
            
            // HorizontalNumberPicker 설정
            val durationPicker = dialogView.findViewById<com.app.pomodoro.ui.view.HorizontalNumberPicker>(R.id.numberPickerDuration)
            durationPicker.setMinValue(1)
            durationPicker.setMaxValue(120)
            durationPicker.setValue(session.duration)
            
            // 세션 타입 선택 버튼
            val btnWorkType = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnWorkType)
            val btnBreakType = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBreakType)
            
                         // 완료 상태 선택 버튼
             val btnCompleted = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCompleted)
             val btnCancelled = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelled)
             
             // 삭제 버튼
             val btnDelete = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)
            
            // 초기 상태 설정
            var selectedSessionType = session.sessionType
            var selectedIsCompleted = session.isCompleted
            
            // 세션 타입 버튼 클릭 리스너
            btnWorkType.setOnClickListener {
                selectedSessionType = SessionType.WORK
                btnWorkType.setBackgroundColor(context.getColor(R.color.primary_blue))
                btnWorkType.setTextColor(context.getColor(android.R.color.white))
                btnBreakType.setBackgroundColor(android.R.color.transparent)
                btnBreakType.setTextColor(context.getColor(R.color.break_mode))
            }
            
            btnBreakType.setOnClickListener {
                selectedSessionType = SessionType.BREAK
                btnBreakType.setBackgroundColor(context.getColor(R.color.break_mode))
                btnBreakType.setTextColor(context.getColor(android.R.color.white))
                btnWorkType.setBackgroundColor(android.R.color.transparent)
                btnWorkType.setTextColor(context.getColor(R.color.primary_blue))
            }
            
            // 완료 상태 버튼 클릭 리스너
            btnCompleted.setOnClickListener {
                selectedIsCompleted = true
                btnCompleted.setBackgroundColor(context.getColor(R.color.success))
                btnCompleted.setTextColor(context.getColor(android.R.color.white))
                btnCancelled.setBackgroundColor(android.R.color.transparent)
                btnCancelled.setTextColor(context.getColor(R.color.error))
            }
            
            btnCancelled.setOnClickListener {
                selectedIsCompleted = false
                btnCancelled.setBackgroundColor(context.getColor(R.color.error))
                btnCancelled.setTextColor(context.getColor(android.R.color.white))
                btnCompleted.setBackgroundColor(android.R.color.transparent)
                btnCompleted.setTextColor(context.getColor(R.color.success))
            }
            
            // 초기 상태 설정
            when (session.sessionType) {
                SessionType.WORK -> {
                    btnWorkType.setBackgroundColor(context.getColor(R.color.primary_blue))
                    btnWorkType.setTextColor(context.getColor(android.R.color.white))
                }
                SessionType.BREAK -> {
                    btnBreakType.setBackgroundColor(context.getColor(R.color.break_mode))
                    btnBreakType.setTextColor(context.getColor(android.R.color.white))
                }
            }
            
            if (session.isCompleted) {
                btnCompleted.setBackgroundColor(context.getColor(R.color.success))
                btnCompleted.setTextColor(context.getColor(android.R.color.white))
            } else {
                btnCancelled.setBackgroundColor(context.getColor(R.color.error))
                btnCancelled.setTextColor(context.getColor(android.R.color.white))
            }
            
            val editDialog = AlertDialog.Builder(context, R.style.DarkDialogTheme)
                .setTitle("항목 수정")
                .setView(dialogView)
                .setPositiveButton("저장") { _, _ ->
                    val newDuration = durationPicker.getValue()
                    
                    val updatedSession = session.copy(
                        duration = newDuration,
                        sessionType = selectedSessionType,
                        isCompleted = selectedIsCompleted
                    )
                    
                    onSessionEdit?.invoke(updatedSession)
                }
                .setNegativeButton("취소", null)
                .create()
            
            // 삭제 버튼 클릭 리스너 (editDialog 생성 후에 설정)
            btnDelete.setOnClickListener {
                onSessionDelete?.invoke(session)
                editDialog.dismiss() // 다이얼로그 닫기
            }
            
            editDialog.window?.setBackgroundDrawableResource(R.color.background_dark)
            editDialog.show()
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