package com.a2t.myapplication.main.ui

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.App
import com.a2t.myapplication.R
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.model.BufferItem
import com.a2t.myapplication.main.presentation.model.SpecialMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainAdapter (
    private val mah: MainAdapterCallback
) : RecyclerView.Adapter<MainViewHolder> () {

    val records = ArrayList<ListRecord>()
    val buffer = ArrayList<BufferItem>()
    var specialMode = SpecialMode.NORMAL
    var isKeyboardON = false
    private val flipAnimator1: Animator = AnimatorInflater.loadAnimator(App.appContext, R.animator.flip_1)
    private val flipAnimator2: Animator = AnimatorInflater.loadAnimator(App.appContext, R.animator.flip_2)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_main, parent, false)
        return MainViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val item = records[position]
        val bufferItem = getBufferItemById(item.id)
        holder.bind(item)
        holder.llAction.isVisible = true
        if (specialMode == SpecialMode.NORMAL) {
            holder.checkbox.isClickable = true
            holder.checkbox.isEnabled = true
        } else {
            holder.checkbox.isClickable = false
            holder.checkbox.isEnabled = false
        }
        when (specialMode) {
            SpecialMode.NORMAL -> {
                holder.ivAction.setImageResource(R.drawable.ic_finger)
                if (item.isNew) {
                    holder.checkbox.isClickable = false
                    holder.checkbox.isEnabled = false
                    holder.llAction.isVisible = false
                    if (item.startEdit) {
                        item.startEdit = false
                        startEditMode(item, holder)
                    }
                }
                // ############################################## РЕАКЦИЯ ОБЪЕКТОВ FOREGROUND ###################################################
                holder.llAction.setOnTouchListener { _: View?, event: MotionEvent ->
                    if (specialMode == SpecialMode.NORMAL && event.action == MotionEvent.ACTION_DOWN) {
                        mah.onStartDrag(holder)
                    }
                    false
                }
                holder.llForeground.setOnClickListener {
                    if (item.isDir) {
                        isKeyboardON = false
                        //mah.goToChildDir(item.id)
                    } else {
                        if (!item.isEdit) startEditMode(item, holder)
                    }
                }


                // ######################################## РЕАКЦИЯ ТЕКСТОВЫХ ПОЛЕЙ ############################################################

                // Отклик поля RECORD на нажатие клавиши ОК клавиатуры - переход в поле PRIM или завершения редактирования
                holder.aetRecord.setOnEditorActionListener { _, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE || (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                        if (holder.aetRecord.getText().toString().isEmpty()) {          // Если поле RECORD пустое
                            endEditMode(item, holder, false)                        // Завершения редактирования
                        } else {                                                        // Если поле RECORD не пустое
                            holder.aetNote.requestFocus()                               // переход в поле PRIM
                        }
                    }
                    true
                }


                // Отклик поля PRIM на нажатие клавиши ОК клавиатуры - завершения редактирования
                holder.aetNote.setOnEditorActionListener { _, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE || (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                        endEditMode(item, holder, true)
                    }
                    true
                }


                // Отклик поля RECORD на потерю фокуса - завершения редактирования
                holder.aetRecord.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus && !holder.aetNote.hasFocus() && item.isEdit)     // Если фокус перешел не в поле NOTE,
                        endEditMode(item, holder, false) //                     // завершить редактирование
                }

                // Отклик поля PRIM на получение фокуса - проверка пуст.строка/потерю - завершения редактирования
                holder.aetNote.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        if (holder.aetRecord.getText().toString().isEmpty()) {      // Если строка RECORD пустая,
                            holder.aetRecord.requestFocus()                         // перевести фокус в поле RECORD
                        }
                    } else {
                        if (!holder.aetRecord.hasFocus() && item.isEdit) { // Если фокус перешел не в поле RECORD Завершить редактирование
                            endEditMode(item, holder, false)
                        }
                    }
                }


            }
            SpecialMode.MOVE -> {
                holder.ivAction.setImageResource(R.drawable.ic_menu)
                if (bufferItem != null) {
                    when(bufferItem.action){
                        1 -> flipPicture(holder.ivAction, R.drawable.ic_cut_red)
                        2 -> flipPicture(holder.ivAction, R.drawable.ic_copy_red)
                        else -> {}
                    }
                }



            }
            SpecialMode.DELETE -> {
                holder.ivAction.setImageResource(R.drawable.ic_basket_white)
                if (bufferItem != null) {
                    flipPicture(holder.ivAction, R.drawable.ic_del_mode)
                }




            }
            SpecialMode.RESTORE -> {
                holder.ivAction.setImageResource(R.drawable.ic_basket_white)
                if (bufferItem != null) {
                    flipPicture(holder.ivAction, R.drawable.ic_rest_mode)
                }




            }
            SpecialMode.ARCHIVE -> {
                holder.ivAction.setImageResource(R.drawable.ic_archive_trans)
                if (item.isArchive) {
                    flipPicture(holder.ivAction, R.drawable.ic_archive_red)
                }




            }
        }






        //holder.itemView.setOnClickListener { clickListener.onTrackClick(item) }
    }

    // Анимация переворота иконки вокруг Y
    private fun flipPicture(view: ImageView, newIcon: Int) {
        try {
            view.cameraDistance = 15000f
            flipAnimator1.setTarget(view)
            flipAnimator1.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    // Сменить картинку
                    view.setImageResource(newIcon)
                    flipAnimator2.setTarget(view)
                    flipAnimator2.start()
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            flipAnimator1.start()
        } catch (e: Exception) {
            view.setImageResource(newIcon)
        }
    }

    private fun getBufferItemById (idToFind: Long): BufferItem? {
        return buffer.find { it.id == idToFind }
    }

    // Старт режима редактирования
    private fun startEditMode(item: ListRecord, holder: MainViewHolder) {
        item.isEdit = true
        holder.aetNote.isVisible = true

        // Разблокируем поля RECORD и PRIM и переводим фокус в поле RECORD
        enableEditText(holder.aetRecord, true)
        enableEditText(holder.aetNote, true)
        holder.aetRecord.requestFocus()
        holder.aetRecord.setSelection(holder.aetRecord.getText()!!.length) // Курсор в конец строки
        if (!isKeyboardON) {                                                                              // Если клавиатуры нет на экране
            showKeyboard(holder.aetRecord, true) // вывести клавиатуру
            isKeyboardON = true
        }

    }

    private fun endEditMode(item: ListRecord, holder: MainViewHolder, em: Boolean) {
        if (item.isEdit) {                // Если включен режим редактирования
            item.isEdit = false // Выключаем режим редактирования
            // Отбрасываем начальные и конечные пробелы в полях RECORD и PRIM
            var str: String = holder.aetRecord.text.toString().trim()
            holder.aetRecord.setText(str)
            str = holder.aetNote.getText().toString().trim()
            holder.aetNote.setText(str)
            isKeyboardON = false
            val maxNpp = item.npp
            if (item.isNew) {      // Новая строка
                if (holder.aetRecord.getText().toString().isNotEmpty()) {     // строка не пустая
                    item.record = holder.aetRecord.getText().toString()
                    item.note = holder.aetNote.getText().toString()
                    item.isNew = false
                    item.lastEditTime = System.currentTimeMillis()
                    mah.insertNewRecord(item)                               // Запись в БД
                    // Создание новой строки
                    val record = ListRecord(
                        0, mah.getIdCurrentDir(), false, maxNpp + 1, false,
                        "", "", 0, 0, 0, 0,
                        null, null, isArchive = false, isDelete = false, isFull = false,
                        isAllCheck = false, true, em, false
                    )
                    if (em) isKeyboardON = true
                    records.add(record)
                    val position: Int = records.size - 1
                    notifyItemInserted(position)
                    notifyItemRangeChanged(position, 1)
                }
            } else {   // Старая строка
                if (holder.aetRecord.getText().toString().isEmpty()) {  // Если aetRecord пустое,
                    holder.aetRecord.setText(item.record)          // вернуть старое значение строки из массива
                }

                if (holder.aetRecord.getText().toString() != item.record || holder.aetNote.getText().toString() != item.record) { // Новые значения не равны старым
                    // Обновляем параметры элемента массива и холдер
                    item.record = holder.aetRecord.getText().toString()
                    item.note = holder.aetNote.getText().toString()
                    item.lastEditTime = System.currentTimeMillis()
                    holder.bind(item)
                    // Сохранение в БД
                    //mah.updateRecord(item)
                }
            }
            // Заполнение поля PRIM
            if (item.note.isEmpty()) {
                holder.aetNote.isVisible = false
            } else {
                holder.aetNote.isVisible = true
                holder.aetNote.setText(item.note)
            }
        }
        if (!isKeyboardON) {
            showKeyboard(holder.aetRecord, false) // Убрать клавиатуру
            isKeyboardON = false
        }
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            enableEditText(holder.aetRecord, false)
            enableEditText(holder.aetNote, false)
        }
    }

    // Открытие/закрытие доступа к полю EditText
    private fun enableEditText(et: EditText, enable: Boolean) {
        et.isEnabled = enable
        et.isClickable = enable
        et.isLongClickable = enable
    }

    // Вывести/убрать клавиатуру
    private fun showKeyboard(et: EditText, show: Boolean) {
        val imm = checkNotNull(App.appContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        CoroutineScope(Dispatchers.Main).launch {
            delay(10)
            if (show) {      // Вывести клавиатуру
                imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
            } else {            // Убрать клавиатуру
                imm.hideSoftInputFromWindow(et.windowToken, 0)
            }
        }
    }

    override fun getItemCount() = records.size


}