package com.a2t.myapplication.main.ui.activity.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.App
import com.a2t.myapplication.R
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.ui.activity.model.SpecialMode
import com.a2t.myapplication.main.ui.fragments.AlarmFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainAdapter(
    private val mac: MainAdapterCallback
) : RecyclerView.Adapter<MainViewHolder>() {

    private var jobBellFull = Job() as Job
    private var jobDebounce = Job() as Job
    val records = ArrayList<ListRecord>()
    var specialMode = SpecialMode.NORMAL
    var isKeyboardON = false
    var currentHolderIdLiveData = MutableLiveData(-1L)
    var currentItem: ListRecord? = null
    var currentHolderPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_main, parent, false)
        return MainViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        //Приведение холдера в исходное состояние
        holder.checkbox.isClickable = false
        holder.checkbox.isEnabled = false
        holder.llForeground.setOnClickListener(null)
        holder.llForeground.setOnLongClickListener(null)
        holder.checkbox.setOnClickListener(null)
        holder.clCheckbox.setOnClickListener(null)
        holder.llAction.setOnTouchListener(null)
        holder.llAction.setOnClickListener(null)

        val item = records[position]
        holder.bind(item)
        App.getTextSizeLiveData().observeForever(holder.observerTextSize!!)
        if (specialMode == SpecialMode.NORMAL || specialMode == SpecialMode.MOVE) {
            holder.observerId = Observer { currentHolderId ->
                if (currentHolderId == holder.id) {
                    holder.ivAction.setBackgroundResource(R.drawable.rect_fon_red)
                } else {
                    holder.ivAction.setBackgroundResource(R.drawable.rect_fon_blue)
                }
            }
            currentHolderIdLiveData.observeForever(holder.observerId!!)
        } else {
            holder.observerId = null
        }

        holder.llAction.isVisible = true
        if (specialMode == SpecialMode.NORMAL) {
            holder.checkbox.isClickable = true
            holder.checkbox.isEnabled = true
        } else {
            holder.checkbox.isClickable = false
            holder.checkbox.isEnabled = false
        }

        // Касание колокольчика показывает время установки напоминания
        holder.ivBell.setOnTouchListener { _: View?, event: MotionEvent ->
            mac.requestMenuFocus()
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> holder.llBellFull.isVisible = true                           // Выводит на экран сообщение о напоминании
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> holder.llBellFull.isVisible = false // Скрывает сообщение о напоминании
                else -> {}
            }
            holder.llBellFull.isVisible
        }

        when(specialMode) {
            //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ Режим NORMAL $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
            SpecialMode.NORMAL -> {
                holder.ivAction.isVisible = true
                holder.ivAction.setImageResource(R.drawable.ic_finger)
                if (item.isNew) {
                    holder.checkbox.isClickable = false
                    holder.checkbox.isEnabled = false
                    holder.llAction.isVisible = false
                    if (item.startEdit) {
                        startEditMode(item, holder)
                        item.startEdit = false
                    }
                }
                // ############################################## РЕАКЦИЯ ОБЪЕКТОВ BACKGROUND ###################################################
                // Потеря фокуса background приводит к откату сдвига Foreground
                holder.llBackground.setOnFocusChangeListener{ _: View?, hasFocus: Boolean ->
                    if (!hasFocus) {
                        mac.returnHolderToOriginalState(holder)
                    }
                }

                holder.ivBtnDel.setOnClickListener {            // Удалить запись
                    holder.llForeground.startAnimation(
                        AnimationUtils.loadAnimation(App.appContext, R.anim.del_holder)
                    ) // Анимация удаления
                    mac.deleteRecords(arrayListOf(item))
                }

                holder.ivBtnEdit.setOnClickListener {            // Редактировать запись
                    mac.requestMenuFocus()
                    startEditMode(item, holder)
                }
                holder.ivBtnBell.setOnClickListener {            // Создать/редактировать напоминание
                    mac.requestMenuFocus()
                    selectCurrentHolder(item, position)
                    mac.passRecordToAlarmFragment(item)
                    (mac as MainActivity).fragmentManager.beginTransaction().setTransition(TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.container_view, AlarmFragment())
                        .addToBackStack("alarmFragment").commit()
                }
                holder.ivBtnDir.setOnClickListener {            // строка <-> папка
                    mac.requestMenuFocus()
                    item.isDir = !item.isDir
                    holder.bind(item)
                    mac.updateRecord(item)
                }
                // ############################################## РЕАКЦИЯ ОБЪЕКТОВ FOREGROUND ###################################################
                holder.llForeground.setOnClickListener {
                    holdersResponseToClick(holder, item)
                }
                // Открытие меню форматирования текста
                holder.llForeground.setOnLongClickListener{
                    selectCurrentHolder(item, position)
                    if (!item.isNew) mac.showContextMenuFormat(holder)    // Не для новой строки
                    true
                }

                // Изменения в CheckBox
                holder.checkbox.setOnClickListener {
                    clickCheckbox(holder, item)
                }
                holder.clCheckbox.setOnClickListener{
                    holder.checkbox.isChecked = !holder.checkbox.isChecked
                    clickCheckbox(holder, item)
                }

                // Перетаскивание
                holder.llAction.setOnTouchListener { _: View?, event: MotionEvent ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        mac.onStartDrag(holder)
                    }
                    false
                }

                // ######################################## РЕАКЦИЯ ТЕКСТОВЫХ ПОЛЕЙ ############################################################
                // Отклик поля RECORD на нажатие клавиши ОК клавиатуры
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
                // Отклик поля NOTE на нажатие клавиши ОК клавиатуры
                holder.aetNote.setOnEditorActionListener { _, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event.action == KeyEvent.ACTION_DOWN
                        && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                        endEditMode(item, holder, true)                         // Завершения редактирования
                    }
                    true
                }
                // Отклик поля RECORD на потерю фокуса - завершения редактирования
                holder.aetRecord.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus && !holder.aetNote.hasFocus() && item.isEdit)     // Если фокус перешел не в поле NOTE,
                        endEditMode(item, holder, false) //                     // завершить редактирование
                }
                // Отклик поля NOTE на получение фокуса - проверка пуст.строка/потерю - завершения редактирования
                holder.aetNote.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        if (holder.aetRecord.getText().toString().isEmpty()) {       // Если строка RECORD пустая,
                            holder.aetRecord.requestFocus()                         // перевести фокус в поле RECORD
                        }
                    } else if (!holder.aetRecord.hasFocus() && item.isEdit) {           // Если фокус перешел не в поле RECORD
                            endEditMode(item, holder, false)                    // Завершить редактирование
                    }
                }
            }
            //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ Режим MOVE $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
            SpecialMode.MOVE -> {
                holder.ivAction.setImageResource(R.drawable.ic_menu)
                if (mac.getMainBuffer().any { it.id == item.id }) {
                    holder.ivAction.setImageResource(R.drawable.ic_copy_red)
                } else if (mac.getMoveBuffer().any { it.id == item.id }) {
                    holder.ivAction.setImageResource(R.drawable.ic_cut_red)
                }

                holder.llForeground.setOnClickListener {
                    if (item.isDir) {
                        isKeyboardON = false
                        mac.goToChildDir(item.id)
                    }
                }

                // Открытие меню Вырезать/Копировать
                holder.llAction.setOnClickListener{
                    selectCurrentHolder(item, position)
                    mac.showContextMenuMove(holder)    // Не для новой строки
                }



            }
            //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ Режим DELETE $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
            SpecialMode.DELETE -> {
                holder.ivAction.setImageResource(R.drawable.ic_basket_white)
                if (mac.getMainBuffer().any { it.id == item.id }) {
                    holder.ivAction.setImageResource(R.drawable.ic_del_mode)
                }

                // Выбор/отмена записи для удаления
                holder.llAction.setOnClickListener{
                    if (mac.getMainBuffer().removeAll { it.id == item.id }) {
                        holder.ivAction.setImageResource(R.drawable.ic_basket_white)
                    } else {
                        mac.getMainBuffer().add(item)
                        holder.ivAction.setImageResource(R.drawable.ic_del_mode)
                    }
                    mac.showNumberOfSelectedRecords()
                }
            }
            //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ Режим RESTORE $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
            SpecialMode.RESTORE -> {
                holder.ivAction.setImageResource(R.drawable.ic_basket_white)
                if (mac.getMainBuffer().any { it.id == item.id }) {
                    holder.ivAction.setImageResource(R.drawable.ic_rest_mode)
                }

                // Выбор/отмена записи для восстановления
                holder.llAction.setOnClickListener{
                    if (mac.getMainBuffer().removeAll { it.id == item.id }) {
                        holder.ivAction.setImageResource(R.drawable.ic_basket_white)
                    } else {
                        mac.getMainBuffer().add(item)
                        holder.ivAction.setImageResource(R.drawable.ic_rest_mode)
                    }
                    mac.showNumberOfSelectedRecords()
                }
            }
            //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ Режим ARCHIVE $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
            SpecialMode.ARCHIVE -> {
                if (item.isDir) {
                    holder.ivAction.isVisible = true
                    holder.ivAction.setImageResource(R.drawable.ic_archive_trans)
                    if (item.isArchive) holder.ivAction.setImageResource(R.drawable.ic_archive_red)
                } else {
                    holder.ivAction.isVisible = false
                }

                holder.llForeground.setOnClickListener {
                    if (item.isDir) {
                        isKeyboardON = false
                        mac.goToChildDir(item.id)
                    }
                }

                // Архивирование/разархивирование
                holder.llAction.setOnClickListener{
                    item.isArchive = !item.isArchive
                    mac.updateRecord(item)
                    val res = if (item.isArchive) R.drawable.ic_archive_red else R.drawable.ic_archive_trans
                    holder.ivAction.setImageResource(res)
                }
            }
        }
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
        if (item.isEdit) {                  // Если включен режим редактирования
            item.isEdit = false             // Выключаем режим редактирования
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
                    holder.bind(item)
                    mac.insertNewRecord(item)                               // Запись в БД
                    // Создание новой строки
                    val record = ListRecord(
                        0, mac.getIdCurrentDir(), false, maxNpp + 1, false,
                        "", "", 0, 0, 0, 0,
                        null, null, null, isArchive = false, isDelete = false, isFull = false,
                        isAllCheck = false, true, em, false
                    )
                    if (em) isKeyboardON = true
                    records.add(record)
                    val position = records.size - 1
                    notifyItemInserted(position)
                    notifyItemRangeChanged(position -1, 2)
                    mac.updateFieldsOfSmallToolbar()
                }
            } else {   // Старая строка
                if (holder.aetRecord.getText().toString().isEmpty()) {  // Если aetRecord пустое,
                    holder.aetRecord.setText(item.record)          // вернуть старое значение строки из массива
                } else if (holder.aetRecord.getText().toString() != item.record
                    || holder.aetNote.getText().toString() != item.record) { // Новые значения не равны старым
                    // Обновляем параметры элемента массива и холдер
                    item.record = holder.aetRecord.getText().toString()
                    item.note = holder.aetNote.getText().toString()
                    item.lastEditTime = System.currentTimeMillis()
                    holder.bind(item)
                    mac.updateRecord(item)  // Сохранение в БД
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
            delay(10)
            enableEditText(holder.aetRecord, false)
            enableEditText(holder.aetNote, false)
            mac.correctingPositionOfRecordByCheck(holder)
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

    private fun clickCheckbox(holder: MainViewHolder, item: ListRecord) {
        mac.requestMenuFocus()
        item.isChecked = holder.checkbox.isChecked
        item.lastEditTime = System.currentTimeMillis()
        holder.bind(item)
        mac.correctingPositionOfRecordByCheck(holder)
        mac.updateRecord(item)  // Сохранение в БД
    }

    override fun onViewRecycled(holder: MainViewHolder) {
        super.onViewRecycled(holder)
        holder.observerId?.let { observer ->
            currentHolderIdLiveData.removeObserver(observer)
            holder.observerId = null // Обнулите ссылку на наблюдателя
        }
        holder.observerTextSize?.let { observer ->
            App.getTextSizeLiveData().removeObserver(observer)
            holder.observerTextSize = null
        }
        jobDebounce.cancel()
        jobBellFull.cancel()
    }

    private fun selectCurrentHolder (item: ListRecord, position: Int) {
        currentHolderIdLiveData.postValue(item.id)
        currentItem = item
        currentHolderPosition = position
    }

    private fun holdersResponseToClick (holder: MainViewHolder, item: ListRecord) {
        if (item.isDir) {
            isKeyboardON = false
            mac.requestMenuFocus()
            mac.goToChildDir(item.id)
        } else {
            if (!item.isEdit) startEditMode(item, holder)
        }
    }

    override fun getItemCount() = records.size
}