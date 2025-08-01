package com.app.pomodoro.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.app.pomodoro.R
import com.app.pomodoro.data.model.TimerState
import kotlin.math.*

/**
 * 뽀모도로 타이머용 원형 커스텀 뷰
 * 사용자의 요구사항에 따라 크고 굵은 원형 시계 형태로 구현
 * 분침이 돌아가는 애니메이션 포함
 */
class CircularTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // 페인트 객체들
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val minuteMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hourMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // 색상
    private var backgroundColor = Color.WHITE
    private var progressColor = Color.BLUE
    private var textColor = Color.BLACK
    private var handColor = Color.RED
    
    // 크기 관련
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var strokeWidth = 20f
    
    // 타이머 관련
    private var totalTime = 25 * 60 // 25분을 초로 변환
    private var currentTime = totalTime
    private var timerState = TimerState.IDLE
    
    // 애니메이션 관련
    private var handAnimator: ValueAnimator? = null
    private var currentHandAngle = -90f // 12시 방향부터 시작
    
    // 진행률 (0.0 ~ 1.0)
    private var progress = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }
    
    init {
        setupPaints()
        loadColors()
    }
    
    private fun setupPaints() {
        // 배경 원
        backgroundPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = this@CircularTimerView.strokeWidth
            color = backgroundColor
        }
        
        // 진행률 원
        progressPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = this@CircularTimerView.strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = progressColor
        }
        
        // 텍스트
        textPaint.apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        // 분 표시
        minuteMarkPaint.apply {
            color = textColor
            strokeWidth = 3f
            alpha = 100
        }
        
        // 시간 표시
        hourMarkPaint.apply {
            color = textColor
            strokeWidth = 6f
        }
        
        // 시계바늘
        handPaint.apply {
            color = handColor
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
        }
    }
    
    private fun loadColors() {
        backgroundColor = ContextCompat.getColor(context, R.color.timer_background)
        progressColor = ContextCompat.getColor(context, R.color.timer_progress)
        textColor = ContextCompat.getColor(context, R.color.timer_text)
        handColor = ContextCompat.getColor(context, R.color.work_mode)
        
        // 페인트 색상 업데이트
        backgroundPaint.color = backgroundColor
        progressPaint.color = progressColor
        textPaint.color = textColor
        minuteMarkPaint.color = textColor
        hourMarkPaint.color = textColor
        handPaint.color = handColor
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        centerX = w / 2f
        centerY = h / 2f
        radius = (min(w, h) / 2f) - strokeWidth - 50f // 여백 고려
        
        // 텍스트 크기를 반지름에 맞게 조정
        textPaint.textSize = radius / 4f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        drawBackground(canvas)
        drawTimeMarks(canvas)
        drawProgress(canvas)
        drawHand(canvas)
        drawCenterText(canvas)
    }
    
    /**
     * 배경 원 그리기
     */
    private fun drawBackground(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
    }
    
    /**
     * 시간 표시 마크 그리기 (시계처럼)
     */
    private fun drawTimeMarks(canvas: Canvas) {
        for (i in 0 until 60) {
            val angle = i * 6f - 90f // 6도씩 (360/60), 12시 방향부터 시작
            val isHourMark = i % 5 == 0
            
            val paint = if (isHourMark) hourMarkPaint else minuteMarkPaint
            val markLength = if (isHourMark) 40f else 20f
            
            val startRadius = radius - markLength
            val endRadius = radius
            
            val startX = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * startRadius
            val startY = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * startRadius
            val endX = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * endRadius
            val endY = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * endRadius
            
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }
    
    /**
     * 진행률 호 그리기
     */
    private fun drawProgress(canvas: Canvas) {
        val sweepAngle = 360f * progress
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
    }
    
    /**
     * 시계바늘 그리기 (분침처럼 동작)
     */
    private fun drawHand(canvas: Canvas) {
        val handLength = radius * 0.7f
        val handEndX = centerX + cos(Math.toRadians(currentHandAngle.toDouble())).toFloat() * handLength
        val handEndY = centerY + sin(Math.toRadians(currentHandAngle.toDouble())).toFloat() * handLength
        
        canvas.drawLine(centerX, centerY, handEndX, handEndY, handPaint)
        
        // 중앙 점 그리기
        canvas.drawCircle(centerX, centerY, 15f, handPaint)
    }
    
    /**
     * 중앙 텍스트 (시간) 그리기
     */
    private fun drawCenterText(canvas: Canvas) {
        val minutes = currentTime / 60
        val seconds = currentTime % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        
        // 텍스트 높이 계산
        val textBounds = Rect()
        textPaint.getTextBounds(timeText, 0, timeText.length, textBounds)
        val textHeight = textBounds.height()
        
        canvas.drawText(timeText, centerX, centerY + textHeight / 2f, textPaint)
    }
    
    /**
     * 타이머 시간 설정
     */
    fun setTotalTime(timeInSeconds: Int) {
        totalTime = timeInSeconds
        currentTime = timeInSeconds
        progress = 0f
        updateHandPosition()
        invalidate()
    }
    
    /**
     * 현재 시간 업데이트
     */
    fun updateTime(timeInSeconds: Int) {
        currentTime = timeInSeconds
        progress = if (totalTime > 0) {
            1f - (currentTime.toFloat() / totalTime.toFloat())
        } else {
            0f
        }
        updateHandPosition()
        invalidate()
    }
    
    /**
     * 타이머 상태 설정
     */
    fun setTimerState(state: TimerState) {
        timerState = state
        
        when (state) {
            TimerState.RUNNING -> {
                startHandAnimation()
                handColor = ContextCompat.getColor(context, R.color.work_mode)
            }
            TimerState.PAUSED -> {
                stopHandAnimation()
                handColor = ContextCompat.getColor(context, R.color.warning)
            }
            TimerState.COMPLETED -> {
                stopHandAnimation()
                handColor = ContextCompat.getColor(context, R.color.success)
            }
            TimerState.IDLE -> {
                stopHandAnimation()
                handColor = ContextCompat.getColor(context, R.color.timer_text)
            }
        }
        
        handPaint.color = handColor
        invalidate()
    }
    
    /**
     * 시계바늘 위치 업데이트
     */
    private fun updateHandPosition() {
        // 시계처럼 동작: 시간이 지날수록 시계방향으로 회전
        val angle = if (totalTime > 0) {
            360f * progress - 90f // 12시 방향부터 시작
        } else {
            -90f
        }
        currentHandAngle = angle
    }
    
    /**
     * 시계바늘 애니메이션 시작
     */
    private fun startHandAnimation() {
        handAnimator?.cancel()
        
        // 부드러운 틱톡 애니메이션
        handAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000 // 1초
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Float
                // 미세한 진동 효과
                val offset = sin(animatedValue * 2 * PI).toFloat() * 2f
                invalidate()
            }
            start()
        }
    }
    
    /**
     * 시계바늘 애니메이션 정지
     */
    private fun stopHandAnimation() {
        handAnimator?.cancel()
        handAnimator = null
    }
    
    /**
     * 작업/휴식 모드에 따른 색상 변경
     */
    fun setWorkMode(isWorkMode: Boolean) {
        val newProgressColor = if (isWorkMode) {
            ContextCompat.getColor(context, R.color.work_mode)
        } else {
            ContextCompat.getColor(context, R.color.break_mode)
        }
        
        progressPaint.color = newProgressColor
        invalidate()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopHandAnimation()
    }
}