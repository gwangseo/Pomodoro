package com.app.pomodoro.ui.view

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.app.pomodoro.R

class HorizontalNumberPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var minValue = 0
    private var maxValue = 100
    private var currentValue = 0
    private var onValueChangedListener: ((Int) -> Unit)? = null
    
    private val scrollView: HorizontalScrollView
    private val numberContainer: LinearLayout
    private val textViews = mutableListOf<TextView>()
    
    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.background_dark))
        
        scrollView = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(ContextCompat.getColor(context, R.color.background_dark))
        }
        
        numberContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.background_dark))
        }
        
        scrollView.addView(numberContainer)
        addView(scrollView)
        
        updateNumbers()
    }
    
    fun setMinValue(value: Int) {
        minValue = value
        updateNumbers()
    }
    
    fun setMaxValue(value: Int) {
        maxValue = value
        updateNumbers()
    }
    
    fun setValue(value: Int) {
        currentValue = value.coerceIn(minValue, maxValue)
        updateHighlight()
        scrollToValue()
    }
    
    fun getValue(): Int = currentValue
    
    fun setOnValueChangedListener(listener: (Int) -> Unit) {
        onValueChangedListener = listener
    }
    
    private fun updateNumbers() {
        numberContainer.removeAllViews()
        textViews.clear()
        
        for (i in minValue..maxValue) {
            val textView = TextView(context).apply {
                text = i.toString()
                textSize = 18f
                setTextColor(ContextCompat.getColor(context, R.color.timer_text_dark))
                setPadding(32, 16, 32, 16)
                gravity = Gravity.CENTER
                isClickable = true
                setBackgroundColor(ContextCompat.getColor(context, R.color.background_dark))
                
                setOnClickListener {
                    currentValue = i
                    onValueChangedListener?.invoke(i)
                    updateHighlight()
                    scrollToValue()
                }
            }
            numberContainer.addView(textView)
            textViews.add(textView)
        }
        updateHighlight()
    }
    
    private fun updateHighlight() {
        textViews.forEachIndexed { index, textView ->
            val value = minValue + index
            when {
                value == currentValue -> {
                    // 선택된 숫자: 밝은 흰색, 볼드
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    textView.textSize = 20f
                    textView.typeface = Typeface.DEFAULT_BOLD
                    textView.alpha = 1.0f
                }
                kotlin.math.abs(value - currentValue) == 1 -> {
                    // 인접한 숫자: 조금 어두운 색
                    textView.setTextColor(ContextCompat.getColor(context, R.color.timer_text_dark))
                    textView.textSize = 18f
                    textView.typeface = Typeface.DEFAULT
                    textView.alpha = 0.8f
                }
                else -> {
                    // 나머지 숫자: 더 어두운 색
                    textView.setTextColor(ContextCompat.getColor(context, R.color.timer_text_dark))
                    textView.textSize = 16f
                    textView.typeface = Typeface.DEFAULT
                    textView.alpha = 0.5f
                }
            }
        }
    }
    
    private fun scrollToValue() {
        val targetIndex = currentValue - minValue
        if (targetIndex >= 0 && targetIndex < numberContainer.childCount) {
            val targetView = numberContainer.getChildAt(targetIndex)
            scrollView.post {
                scrollView.smoothScrollTo(targetView.left - scrollView.width / 2 + targetView.width / 2, 0)
            }
        }
    }
}
