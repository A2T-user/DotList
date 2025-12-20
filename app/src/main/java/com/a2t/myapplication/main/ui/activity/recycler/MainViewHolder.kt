package com.a2t.myapplication.main.ui.activity.recycler

import android.graphics.Paint
import android.graphics.Typeface
import android.text.format.DateFormat
import android.view.View
import android.view.animation.Animation
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.common.App
import com.a2t.myapplication.R
import com.a2t.myapplication.common.model.DLAnimator
import com.a2t.myapplication.main.ui.ActionEditText
import com.a2t.myapplication.utilities.AlarmHelper
import com.a2t.myapplication.main.domain.model.ListRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var observerId: Observer<Long>? = null
    var observerTextSize: Observer<Float>? = null
    private var endDayJob: Job? = null
    private var alarmJob: Job? = null
    private var animationBell: Animation = DLAnimator().animBell
    var id: Long = 0
    private var bellType = 0        // Тип иконки напоминания (0-скрыта, 1-белый, 2-красный, 3-анимация)
    // foreground
    val llForeground: LinearLayout = itemView.findViewById(R.id.ll_foreground)
    val clCheckbox: ConstraintLayout = itemView.findViewById(R.id.cl_checkbox)
    val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
    private val ivDirIcon: ImageView = itemView.findViewById(R.id.iv_dir_icon)
    private val tvDateTime: TextView = itemView.findViewById(R.id.tv_date_time)
    val aetRecord: ActionEditText = itemView.findViewById(R.id.aet_record)
    val aetNote: ActionEditText = itemView.findViewById(R.id.aet_note)
    val ivBell: ImageView = itemView.findViewById(R.id.iv_bell)
    val llAction: LinearLayout = itemView.findViewById(R.id.ll_action)
    val ivAction: ImageView = itemView.findViewById(R.id.iv_action)
    val llBellFull: LinearLayout = itemView.findViewById(R.id.ll_bell_full)
    private val ivBellFull: ImageView = itemView.findViewById(R.id.iv_bell_full)
    private val tvTimeBellFull: TextView = itemView.findViewById(R.id.tv_time_bell_full)
    // background
    val llBackground: LinearLayout = itemView.findViewById(R.id.ll_background)
    val ivBtnDel: ImageView = itemView.findViewById(R.id.iv_btn_del)
    val ivBtnEdit: ImageView = itemView.findViewById(R.id.iv_btn_edit)
    val ivBtnBell: ImageView = itemView.findViewById(R.id.iv_btn_bell)
    val ivBtnDir: ImageView = itemView.findViewById(R.id.iv_btn_dir)
    val ivBtnImagePlus: ImageView = itemView.findViewById(R.id.iv_btn_image_plus)
    // Слой для выделения записей с checkbox = true
    private val vLayerCheck: View = itemView.findViewById(R.id.v_layer_check)

    fun bind(item: ListRecord) {
        id = item.id
        //background
        // Кнопка Редактирование
        ivBtnEdit.isVisible = item.isDir
        // Кнопка Напоминания
        var res = if (item.alarmTime == null) R.drawable.ic_bell_plus else R.drawable.ic_bell_edit
        // Кнопка Папка
        ivBtnBell.setImageResource(res)
        if (item.isFull) {
            ivBtnDir.isVisible = false
        } else {
            ivBtnDir.isVisible = true
            res = if (item.isDir) R.drawable.ic_dir_off else R.drawable.ic_dir_on
            ivBtnDir.setImageResource(res)
        }
        if (!item.mediaFile.isNullOrEmpty()) ivBtnDir.isVisible = false

        // Кнопка Присоединить медиафайл
        ivBtnImagePlus.isVisible = !item.isDir && item.mediaFile.isNullOrEmpty()

        //foreground
        // Иконка папки/картинки
        val resource = when {
            !item.isFull -> R.drawable.ic_dir_empty
            item.isAllCheck -> R.drawable.ic_dir_check
            else -> R.drawable.ic_dir_full
        }
        if (item.isDir || !item.mediaFile.isNullOrEmpty()) {
            ivDirIcon.visibility = View.VISIBLE
            ivDirIcon.setImageResource(if (item.isDir) resource else R.drawable.ic_image)
        } else {
            ivDirIcon.visibility = View.INVISIBLE
        }
        // Чекбокс
        checkbox.isChecked = item.isChecked
        // Подсвечивает строки с установленным чекбокс
        if (item.isChecked) {
            vLayerCheck.visibility = View.VISIBLE
        } else {
            vLayerCheck.visibility = View.INVISIBLE
        }
        // Зачеркивает текст осн.поля строк с установленным чекбокс
        if (item.isChecked && App.appSettings.crossedOutOn) {
            aetRecord.paintFlags = aetRecord.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG           // Зачеркнуть
        } else {
            aetRecord.paintFlags = aetRecord.paintFlags and (Paint.STRIKE_THRU_TEXT_FLAG.inv())  // Снять зачеркивание
        }
        // Поле Дата последнего редактирования
        tvDateTime.text = if (item.lastEditTime != 0L) AlarmHelper.convertDateTime(item.lastEditTime) else ""

        //Установка размера шрифта
        observerTextSize = Observer { size ->
            aetRecord.textSize = size
            aetNote.textSize = 0.75f * size
        }
        // Заполнение полей основного и прмечания
        aetRecord.setText(item.record)
        aetNote.setText(item.note)
        aetNote.isVisible = item.note.isNotEmpty()
        // Установка цвета/стиля шрифта
        changeTextColor(item.textColor)
        changeTextStyle(item.textStyle)
        changeTextUnder(item.textUnder)
        // Установка колокольчика
        changeBellType(item)
        changeBellIcon()
        // Заполнение поля Время напоминания
        item.alarmTime?.let {
            tvTimeBellFull.text = DateFormat.format("EEE, dd.M.yy HH:mm", it)
            .toString()
            .replaceFirstChar { char -> char.uppercase() }
        }
    }

    private fun changeTextColor(textColor: Int) {
        val context = App.appContext
        when(textColor) {
            1 -> {
                aetRecord.setTextColor(ContextCompat.getColor(context, R.color.text_color_1))
                aetNote.setTextColor(ContextCompat.getColor(context, R.color.text_color_1))
            }
            2 -> {
                aetRecord.setTextColor(ContextCompat.getColor(context, R.color.text_color_2))
                aetNote.setTextColor(ContextCompat.getColor(context, R.color.text_color_2))
            }
            3 -> {
                aetRecord.setTextColor(ContextCompat.getColor(context, R.color.text_color_3))
                aetNote.setTextColor(ContextCompat.getColor(context, R.color.text_color_3))
            }
            else -> {
                aetRecord.setTextColor(tvDateTime.textColors)
                aetNote.setTextColor(tvDateTime.textColors)
            }
        }
    }

    private fun changeTextStyle(textStyle: Int) {
        aetRecord.setTypeface(
            null,
            when(textStyle) {
                1 -> Typeface.BOLD
                2 -> Typeface.ITALIC
                3 -> Typeface.BOLD_ITALIC
                else -> Typeface.NORMAL
            }
        )
    }

    // Включение/выключение подчеркивания остовного поля
    private fun changeTextUnder(textUnder: Int) {
        if (textUnder == 1) {
            aetRecord.paintFlags = aetRecord.paintFlags or Paint.UNDERLINE_TEXT_FLAG // Подчеркнуть
        } else {
            aetRecord.paintFlags = aetRecord.paintFlags and (Paint.UNDERLINE_TEXT_FLAG.inv()) // Снять подчеркивание
        }
    }

    // Определение типа иконки колокольчика
    private fun changeBellType(item: ListRecord) {
        alarmJob?.cancel()
        endDayJob?.cancel()
        val alarmTime = item.alarmTime
        val systemTime = System.currentTimeMillis()
        val startCurrentDay = AlarmHelper.startOfCurrentDay()
        val endCurrentDay = AlarmHelper.endOfCurrentDay()
        if (alarmTime == null) {
            bellType = 0
        } else if (alarmTime > endCurrentDay) {
            bellType = 1
        } else if (alarmTime > systemTime) {
            bellType = 2
            alarmJob = CoroutineScope(Dispatchers.Main).launch {
                delay(alarmTime - systemTime)
                bellType = 3
                changeBellIcon()
            }
        } else {
            bellType = 3
        }
        if (alarmTime != null) {
            endDayJob = CoroutineScope(Dispatchers.Main).launch {
                delay(endCurrentDay - alarmTime + 1)
                if (alarmTime <= startCurrentDay) {
                    item.alarmTime = null
                    item.alarmText = null
                }
                changeBellType(item)
                changeBellIcon()
            }
        }
    }

    // Установка иконки колокольчика
    private fun changeBellIcon() {
        when(bellType) {
            1 -> {
                ivBell.isVisible = true
                ivBell.setImageResource(R.drawable.ic_bell_white)
                ivBellFull.setImageResource(R.drawable.ic_bell_white)
            }
            2 -> {
                ivBell.isVisible = true
                ivBell.setImageResource(R.drawable.ic_bell_red)
                ivBellFull.setImageResource(R.drawable.ic_bell_red)
            }
            3 -> {
                ivBell.isVisible = true
                ivBell.setImageResource(R.drawable.ic_bell_alarm)
                ivBellFull.setImageResource(R.drawable.ic_bell_alarm)
                ivBell.startAnimation(animationBell)
                ivBellFull.startAnimation(animationBell)
            }
            else -> ivBell.isVisible = false
        }
    }
}