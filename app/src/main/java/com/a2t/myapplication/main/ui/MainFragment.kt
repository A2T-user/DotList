package com.a2t.myapplication.main.ui

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import com.a2t.myapplication.App
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.ContextMenuFormatBinding
import com.a2t.myapplication.databinding.ContextMenuMoveBinding
import com.a2t.myapplication.databinding.FragmentMainBinding
import com.a2t.myapplication.databinding.ToolbarModesBinding
import com.a2t.myapplication.databinding.ToolbarSideBinding
import com.a2t.myapplication.databinding.ToolbarSmallBinding
import com.a2t.myapplication.databinding.ToolbarTopBinding
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.presentation.model.OpenDirMode
import com.a2t.myapplication.main.presentation.model.SpecialMode
import com.a2t.myapplication.root.ui.RootActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

const val K_MAX_SHIFT_RIGHT = 0.2f
const val K_MAX_SHIFT_LEFT = -0.3f
const val ANIMATION_DELEY = 300L
const val EYE_ANIMATION_DELEY = 5000L

class MainFragment : Fragment(), MainAdapterCallback {
    private val mainViewModel by viewModel<MainViewModel>()
    private val adapter = MainAdapter(this)
    private var mIth: ItemTouchHelper? = null
    private var mIthScb: ItemTouchHelper.Callback? = null
    var idDir = 0L
    private var nameDir = "R:"
    var specialMode = SpecialMode.NORMAL
    private lateinit var binding: FragmentMainBinding
    private lateinit var topToolbarBinding: ToolbarTopBinding
    private lateinit var smallToolbarBinding: ToolbarSmallBinding
    private lateinit var sideToolbarBinding: ToolbarSideBinding
    private lateinit var contextMenuFormatBinding: ContextMenuFormatBinding
    private lateinit var contextMenuMoveBinding: ContextMenuMoveBinding
    private lateinit var modesToolbarBinding: ToolbarModesBinding
    private var isSideToolbarFullShow = false
    private var widthScreen = 0                                    // Ширина экрана
    private var heightScreen = 0                                   // Ширина экрана
    private var maxShiftToRight = 0f                                // Величина максимального смещения при свайпе в право
    private var maxShiftToLeft = 0f                                 // Величина максимального смещения при свайпе в лево
    private var hidhtContextMenu = 0f                               // Высота контекстного меню
    private lateinit var animationMoveMode: Animation
    private lateinit var animationDeleteMode: Animation
    private lateinit var animationRestoreMode: Animation
    private lateinit var animationArchiveMode: Animation
    private lateinit var animationEye: Animation
    private lateinit var animOpenNewDir: LayoutAnimationController
    private lateinit var animOpenChildDir: LayoutAnimationController
    private lateinit var animOpenParentDir: LayoutAnimationController
    private var archiveJob = lifecycleScope.launch {}
    private var eyeJob = lifecycleScope.launch {}
    private var isNoSleepMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        topToolbarBinding = binding.topToolbar
        smallToolbarBinding = binding.smallToolbar
        sideToolbarBinding = binding.sideBar
        contextMenuFormatBinding = binding.contextMenuFormat
        contextMenuMoveBinding = binding.contextMenuMove
        modesToolbarBinding = binding.modesToolbar
        return binding.root
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Необходимые размеры
        // Определяем ширину экрана, пределы смещения холдера вдоль оси Х вправо и влево
        widthScreen = requireContext().resources.displayMetrics.widthPixels
        heightScreen = requireContext().resources.displayMetrics.heightPixels
        val dpSize = this.resources.displayMetrics.density        // Размер dp
        hidhtContextMenu = 56 * dpSize                                  // Высота контекстного меню в px
        maxShiftToRight = widthScreen * K_MAX_SHIFT_RIGHT               // Величина максимального смещения при свайпе в право
        maxShiftToLeft = widthScreen * K_MAX_SHIFT_LEFT                 // Величина максимального смещения при свайпе в лево

        // Анимации
        animationMoveMode = AnimationUtils.loadAnimation(requireContext(), R.anim.arrow_right)
        animationDeleteMode = AnimationUtils.loadAnimation(requireContext(), R.anim.arrow_down)
        animationRestoreMode = AnimationUtils.loadAnimation(requireContext(), R.anim.arrow_up)
        animationArchiveMode = AnimationUtils.loadAnimation(requireContext(), R.anim.archive)
        animationEye = AnimationUtils.loadAnimation(requireContext(), R.anim.eye_eff)
        animOpenNewDir = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.anim_open_new_dir)
        animOpenChildDir = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.anim_open_child_dir)
        animOpenParentDir = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.anim_open_parent_dir)

        initializingRecyclerView ()

        goToNormalMode()

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ ГЛАВНАЯ ПАНЕЛЬ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

        // Кнопка МЕНЮ
        topToolbarBinding.btnMenu.setOnClickListener {
            requestFocusInTouch()                   // Присвоение фокуса
            noSleepModeOff()           // Выключение режима БЕЗ СНА
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment2)
        }

        // НЕ СПЯЩИЙ РЕЖИМ
        topToolbarBinding.imageEye.setOnClickListener {
            requestFocusInTouch()                   // Присвоение фокуса
            if (isNoSleepMode) noSleepModeOff() else noSleepModeON()

        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ БОКОВАЯ ПАНЕЛЬ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // swipe влево по флагу для открытия БОКОВОЙ ПАНЕЛИ
        val downX = AtomicReference( 0f)
        val downY = AtomicReference( 0f)
        val isTouch = AtomicBoolean(false)
        binding.sideBarContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX.set(event.x)
                    downY.set(event.y)
                    isTouch.set(true)
                }
                MotionEvent.ACTION_CANCEL -> isTouch.set(false)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_MOVE -> {
                    if (isTouch.get()) {
                        val dX = event.x - downX.get()
                        val dY = event.y - downY.get()
                        if (abs(dX/dY) > 1.5 && dX < 0) {                    // Если жест горизонталный, влево
                            enableSpecialMode()                                 // Переход в нормальный режим
                            sideBarShowOrHide(true)                       // Открыть боковую панель
                            noSleepModeOff()                                    // Выключение режима БЕЗ СНА
                        }
                    }
                }
            }
            return@setOnTouchListener isTouch.get()
        }
        // Потеря фокуса кнопкой развернуть/свернуть убирает бок.панель
        sideToolbarBinding.llSideBarOpen.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) sideBarShowOrHide(false)
        }

        // Кнопка Развернуть/Свернуть боковую панель
        sideToolbarBinding.llSideBarOpen.setOnClickListener {
            sideBarFullOpenClose(!isSideToolbarFullShow)
        }

        // Кнопка режима Удаления
        sideToolbarBinding.llSideBarDelMode.setOnClickListener {
            Log.e ("МОЁ", "Удаления")
            requestFocusInTouch()
            specialMode = SpecialMode.DELETE
            enableSpecialMode()
            goToDir(OpenDirMode.NEW_DIR)
        }

        // Кнопка режима Восстановления
        sideToolbarBinding.llSideBarRestMode.setOnClickListener {
            Log.e ("МОЁ", "Восстановления")
            requestFocusInTouch()
            specialMode = SpecialMode.RESTORE
            enableSpecialMode()
            goToDir(OpenDirMode.NEW_DIR)
        }

        // Кнопка режима Переноса
        sideToolbarBinding.llSideBarMoveMode.setOnClickListener {
            Log.e ("МОЁ", "Переноса")
            requestFocusInTouch()
            specialMode = SpecialMode.MOVE
            enableSpecialMode()
            goToDir(OpenDirMode.NEW_DIR)
        }

        // Кнопка режима Архив
        sideToolbarBinding.llSideBarArchiveMode.setOnClickListener {
            Log.e ("МОЁ", "Архив")
            requestFocusInTouch()
            specialMode = SpecialMode.ARCHIVE
            enableSpecialMode()
            goToDir(OpenDirMode.NEW_DIR)
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ НИЖНЯЯ ПАНЕЛЬ ИНСТРУМЕНТОВ РЕЖИМЫ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // Свайп вниз закрывает нижнюю панель и переводит рециклер в обычный режим
        modesToolbarBinding.clModesToolbar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX.set(event.x)
                    downY.set(event.y)
                    isTouch.set(true)
                }
                MotionEvent.ACTION_CANCEL -> isTouch.set(false)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_MOVE -> {
                    if (isTouch.get()) {
                        val dX = event.x - downX.get()
                        val dY = event.y - downY.get()
                        if (abs(dY/dX) > 1 && dY > 100) {// Если жест вертикальный, вниз
                            goToNormalMode ()
                        }
                    }
                }
            }
            return@setOnTouchListener isTouch.get()
        }
        // Клик по кнопке Закрыть закрывает нижнюю панель и переводит рециклер в обычный режим
        modesToolbarBinding.btnClose.setOnClickListener {
            goToNormalMode ()
        }
    }

    // Режим БЕЗ СНА
    // Включение режима БЕЗ СНА
    private fun noSleepModeON () {
        isNoSleepMode = true
        (requireActivity() as RootActivity).switchNoSleepMode(true)    // В активити включаем keepScreenOn
        topToolbarBinding.imageEye.setImageResource(R.drawable.ic_eye_open) // Сменить иконку
        topToolbarBinding.imageEye.startAnimation(animationEye)             // Анимация иконки
        // Маргание иконки каждые 10 секунд
        eyeJob = lifecycleScope.launch {
            eyeAnimation()
        }
        (requireActivity() as RootActivity).showHintToast(getString(R.string.no_seep_mode_on), Toast.LENGTH_SHORT)
    }
    // Выключение режима БЕЗ СНА
    private fun noSleepModeOff () {
        if (isNoSleepMode) {
            isNoSleepMode = false
            (requireActivity() as RootActivity).switchNoSleepMode(false)       // В активити выключаем keepScreenOn
            topToolbarBinding.imageEye.setImageResource(R.drawable.ic_eye_closed)   // Сменить иконку
            topToolbarBinding.imageEye.startAnimation(animationEye)                 // Анимация иконки
            eyeJob.cancel()                                                         // Отменить маргание глаза
            (requireActivity() as RootActivity).showHintToast(getString(R.string.no_seep_mode_off), Toast.LENGTH_SHORT)
        }
    }
    // Анимация глаза
    private suspend fun eyeAnimation() {
        delay(EYE_ANIMATION_DELEY)
        topToolbarBinding.imageEye.startAnimation(animationEye)             // Анимация иконки
        eyeAnimation()                                                      // Рекурсия
    }

    // Присвоение фокуса
    override fun requestFocusInTouch () {
        topToolbarBinding.imageEye.isFocusableInTouchMode = true
        topToolbarBinding.imageEye.requestFocus()
        topToolbarBinding.imageEye.isFocusableInTouchMode = false
    }


    // Открытие специального режима
    private fun enableSpecialMode () {
        noSleepModeOff()           // Выключение режима БЕЗ СНА
        topToolbarBinding.imageEye.isVisible = specialMode == SpecialMode.NORMAL
        showSpecialModeToolbar()
    }

    // Показать панель инструментов специального режима
    private fun showSpecialModeToolbar () {
        stopAllModeAnimations()         // Остановить все анимации
        if (specialMode == SpecialMode.NORMAL) {
            modesToolbarBinding.clModesToolbar.isVisible = false
        } else {
            modesToolbarBinding.clModesToolbar.isVisible = true
            when (specialMode) {
                SpecialMode.MOVE -> {
                    modesToolbarBinding.btnSelectAll.isVisible = false
                    modesToolbarBinding.btnAction.isVisible = true
                    modesToolbarBinding.btnAction.text = getString(R.string.insert)
                }
                SpecialMode.DELETE -> {
                    modesToolbarBinding.btnSelectAll.isVisible = true
                    modesToolbarBinding.btnAction.isVisible = true
                    modesToolbarBinding.btnAction.text = getString(R.string.delete)
                }
                SpecialMode.RESTORE -> {
                    modesToolbarBinding.btnSelectAll.isVisible = true
                    modesToolbarBinding.btnAction.isVisible = true
                    modesToolbarBinding.btnAction.text = getString(R.string.restore)
                }
                SpecialMode.ARCHIVE -> {
                    modesToolbarBinding.btnSelectAll.isVisible = false
                    modesToolbarBinding.btnAction.isVisible = false
                }
                SpecialMode.NORMAL -> {}
            }
            showIconMode()
        }
    }

    private fun showIconMode () {
        stopAllModeAnimations()         // Остановить все анимации панели режимов
        when (specialMode) {
            SpecialMode.MOVE -> {
                modesToolbarBinding.ivBarModes3.isVisible = false
                modesToolbarBinding.ivBarModes2.setImageResource(R.drawable.ic_move_mode_2)
                modesToolbarBinding.ivBarModes1.setImageResource(R.drawable.ic_move_mode_1)
                modesToolbarBinding.ivBarModes1.startAnimation(animationMoveMode)               // Анимация
            }
            SpecialMode.DELETE -> {
                modesToolbarBinding.ivBarModes3.isVisible = false
                modesToolbarBinding.ivBarModes2.setImageResource(R.drawable.ic_basket)
                modesToolbarBinding.ivBarModes1.setImageResource(R.drawable.ic_arrow_red)
                modesToolbarBinding.ivBarModes1.startAnimation(animationDeleteMode)             // Анимация
            }
            SpecialMode.RESTORE -> {
                modesToolbarBinding.ivBarModes3.isVisible = false
                modesToolbarBinding.ivBarModes2.setImageResource(R.drawable.ic_basket)
                modesToolbarBinding.ivBarModes1.setImageResource(R.drawable.ic_arrow_blue)
                modesToolbarBinding.ivBarModes1.startAnimation(animationRestoreMode)            // Анимация
            }
            SpecialMode.ARCHIVE -> {
                modesToolbarBinding.ivBarModes3.isVisible = true
                modesToolbarBinding.ivBarModes2.setImageResource(R.drawable.ic_archive_mode_2)
                modesToolbarBinding.ivBarModes1.setImageResource(R.drawable.ic_archive_mode_1)
                // Анимация
                archiveJob = lifecycleScope.launch {
                    archiveModeAnimation()
                }
            }
            SpecialMode.NORMAL -> {}
        }
    }

    // Анимация Архива
    private suspend fun archiveModeAnimation () {
        delay(ANIMATION_DELEY)
        modesToolbarBinding.ivBarModes2.startAnimation(animationArchiveMode)            // Анимация
        delay(ANIMATION_DELEY)
        modesToolbarBinding.ivBarModes3.startAnimation(animationArchiveMode)            // Анимация
        delay(ANIMATION_DELEY)
        modesToolbarBinding.ivBarModes1.startAnimation(animationArchiveMode)            // Анимация
        archiveModeAnimation()                                                              // Рекурсия
    }

    // Остановить все анимации
    private fun stopAllModeAnimations () {
        archiveJob.cancel()
        modesToolbarBinding.ivBarModes1.clearAnimation()
        modesToolbarBinding.ivBarModes2.clearAnimation()
        modesToolbarBinding.ivBarModes3.clearAnimation()
    }






    // Показать/скрыть боковую панель
    private fun sideBarShowOrHide (show: Boolean) {
        if (show) {
            binding.sideBarFlag.isVisible = false                           // Убрать ярлык боковой панели
            sideToolbarBinding.llSideBar.isVisible = true                   // Вывести бок.панель
            // Вернуть кнопку "Развернуть панель" в исходное положение
            sideToolbarBinding.ivSideBarOpen.animate().rotation(0f)   // Кнопку Развернуть панель в исх.положение
            sideToolbarBinding.llSideBarOpen.requestFocus()                 // и перевести фокус на нее
        } else {
            if (isSideToolbarFullShow) sideBarFullOpenClose(false)      // Свернуть бок.панель
            sideToolbarBinding.llSideBar.isVisible = false                  // Убрать бок.панель
            binding.sideBarFlag.isVisible = true
        }
    }

    // Разворачивание/сворачивание боковой панели
    private fun sideBarFullOpenClose (full: Boolean) {
        if (full) {
            sideToolbarBinding.ivSideBarOpen.animate().rotation(180f)     // Перевернуть кнопку Развернуть панель
            showSideBarText(true)                                         // Показать пояснительный текст кнопок
            isSideToolbarFullShow = true
        } else {
            sideToolbarBinding.ivSideBarOpen.animate().rotation(0f)       // Перевернуть кнопку Развернуть панель
            showSideBarText(false)                                        // Убрать пояснительный текст кнопок
            isSideToolbarFullShow = false

        }
    }

    // Показать пояснительный текст кнопок боковой панели
    private fun showSideBarText (show: Boolean) {
        sideToolbarBinding.tvSideBarOpen.isVisible = show                   // Текст кнопки Развернуть панель
        sideToolbarBinding.tvSideBarSend.isVisible = show                   // Текст кнопки Переслать
        sideToolbarBinding.tvSideBarConvertText.isVisible = show            // Текст кнопки Конвертация
        sideToolbarBinding.tvSideBarDelMark.isVisible = show                // Текст кнопки Удалить метки
        sideToolbarBinding.tvSideBarNotifications.isVisible = show          // Текст кнопки Напоминания
        sideToolbarBinding.tvSideBarDelMode.isVisible = show                // Текст кнопки Удаление
        sideToolbarBinding.tvSideBarRestMode.isVisible = show               // Текст кнопки Восстановление
        sideToolbarBinding.tvSideBarMoveMode.isVisible = show               // Текст кнопки Перенос
        sideToolbarBinding.tvSideBarArchiveMode.isVisible = show            // Текст кнопки Архив
    }

    private fun getItemById (id: Long, records: List<ListRecord>): ListRecord? {
        return records.find { it.id == id }
    }

    private fun updateNppInList (list: ArrayList<ListRecord>) {
        for (i in 0 until list.size)
            list[i].npp = i
        // Записать изменения в БД *******************************************************************************************************************************************
    }
    private fun initializingRecyclerView () {
        binding.recycler.adapter = adapter
        binding.recycler.setLayoutManager(object : LinearLayoutManager(requireContext()) {
            // Разрешаем скольжение тоько при старте редактирования записи
            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean,
                focusedChildVisible: Boolean
            ): Boolean {
                if (requireActivity().currentFocus is ActionEditText)
                    super.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible)
                return false
            }
        })
        val itemAnimator: ItemAnimator = DefaultItemAnimator()
        itemAnimator.moveDuration = 300
        itemAnimator.removeDuration = 100
        binding.recycler.setItemAnimator(itemAnimator)
        binding.recycler.scheduleLayoutAnimation()
        binding.recycler.layoutAnimation = animOpenNewDir
        binding.recycler.invalidate()
        mIthScb = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            var isSwipe = false
            var isMove = false
            override fun isLongPressDragEnabled(): Boolean {        // Запретить Drag по LongPress (перетаскивание за контроллер)
                return false
            }
            // Разрешить Swipe
            override fun isItemViewSwipeEnabled(): Boolean {
                return specialMode == SpecialMode.NORMAL            // Swipe будет только в нормальном режиме
            }
            // Сделать свайп грубее
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 1.2f                //по умолчанию 0.5f - перемещение на 1/2 экрана
            }
            override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                return 100 * defaultValue   // Увеличиваем минимальную скорость свайпа
            }
            // Премещает FOREGROUND, оставляя BACKGROUND на месте
            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, dX: Float,
                dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                // Перемещение только Foreground, Background остается на месте
                val holder = viewHolder as MainViewHolder
                val foregroundView = holder.llForeground
                val item = getItemById(holder.id, adapter.records)
                val moveX = foregroundView.x
                val distX: Float
                if (item != null) {
                    if (!item.isNew  && specialMode == SpecialMode.NORMAL) {
                        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                            distX = if (isCurrentlyActive) {
                                if (abs(moveX.toDouble()) < 0.5 * widthScreen) {
                                    dX
                                } else if (moveX >= 0) {
                                    0.5f * widthScreen
                                } else {
                                    -0.5f * widthScreen
                                }
                            } else {
                                if (moveX > 0.0f) {
                                    if (moveX < 0.5f * maxShiftToRight) 0.0f else maxShiftToRight
                                } else {
                                    if (moveX > 0.5f * maxShiftToLeft) 0.0f else maxShiftToLeft
                                }
                            }
                            getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, distX, 0.0f, actionState, isCurrentlyActive)
                            isSwipe = true
                        } else {
                            getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, 0.0f, dY, actionState, isCurrentlyActive)
                        }
                    }
                }
            }
            // Перетаскивание
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val listDir = adapter.records
                val fromPos: Int = listDir.indexOfFirst { it.id == (viewHolder as MainViewHolder).id }
                val toPos: Int = listDir.indexOfFirst { it.id == (target as MainViewHolder).id }
                if (toPos < listDir.size - 1) {
                    isSwipe = false
                    isMove = true
                    if (fromPos < toPos) {                             // Направление перемещения
                        for (i in fromPos until toPos) {         // Премещение элементов
                            Collections.swap(listDir, i, i + 1)     // массива в новые позиции
                        }
                    } else {
                        for (i in fromPos downTo toPos + 1) {     // Премещение элементов
                            Collections.swap(listDir, i, i - 1)      // массива в новые позиции
                        }
                    }
                    updateNppInList(listDir) // Обновить порядковые номера записей в массиве и БД******************************************************************************************
                    // Сообщение Recicler, что элемент перемещен
                    adapter.notifyItemMoved(fromPos, toPos)
                    if (fromPos > toPos) {
                        adapter.notifyItemRangeChanged(toPos, fromPos - toPos + 1)
                    } else {
                        adapter.notifyItemRangeChanged(fromPos, toPos - fromPos + 1)
                    }
                }
                return isMove
            }
            // Смахивание
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            // Перерисовывает ViewHolder после манипуляций
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (isSwipe) {
                    // Переводим фокус на background
                    val backgroundView =
                        (viewHolder as MainViewHolder).llBackground
                    if (!backgroundView.hasFocus()) backgroundView.requestFocus()
                } else {
                    returnHolderToOriginalState(viewHolder)
                }
                // Сортировка по меткам
                if (isMove) correctingPositionOfRecordByCheck(viewHolder as MainViewHolder)
            }
        }
        mIth = ItemTouchHelper(mIthScb!!)
        mIth!!.attachToRecyclerView(binding.recycler)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mIth!!.startDrag(viewHolder)
    }

    // Вызывается из адаптера для возврата Foreground в исходное положение
    override fun returnHolderToOriginalState(viewHolder: RecyclerView.ViewHolder) {
        val foregroundView = (viewHolder as MainViewHolder).llForeground
        ItemTouchHelper.Callback.getDefaultUIUtil().clearView(foregroundView)
    }

    fun mainBackPressed () {
        Log.e ("МОЁ", "mainBackPressed")
        adapter.isKeyboardON = false            // Если нажат Back, клавиатура точно скрыта
        requestFocusInTouch()
        noSleepModeOff()                        // Выключение режима БЕЗ СНА
        goToParentDir ()                        // Переход к родительской папке
    }

    // Возврат в режим NORMAL
    private fun goToNormalMode () {
        requestFocusInTouch()
        specialMode = SpecialMode.NORMAL
        enableSpecialMode()
        goToDir(OpenDirMode.NEW_DIR)
    }

    private fun goToParentDir () {
        mainViewModel.getParentDir(idDir).observe(viewLifecycleOwner) { id ->
            idDir = id[0]
            goToDir(OpenDirMode.PARENT_DIR)
        }
    }

    override fun goToChildDir (id: Long) {
        idDir = id
        goToDir(OpenDirMode.CHILD_DIR)
    }

    private fun goToDir (openDirMode: OpenDirMode) {
        getRecords(specialMode, idDir, openDirMode)
    }

    private fun fillingRecycler(records: List<ListRecord>, openDirMode: OpenDirMode) {
        val animationController = when (openDirMode) {
            OpenDirMode.NEW_DIR -> animOpenNewDir
            OpenDirMode.CHILD_DIR -> animOpenChildDir
            OpenDirMode.PARENT_DIR -> animOpenParentDir
        }
        binding.recycler.layoutAnimation = animationController
        mainViewModel.getNameDir(idDir).observe(viewLifecycleOwner) { names ->
            nameDir = if (names.isEmpty()) "R:" else names[0]
            topToolbarBinding.pathDir.text = nameDir
        }
        adapter.specialMode = specialMode
        adapter.buffer.clear()
        adapter.records.clear()
        adapter.records.addAll(records)
        adapter.notifyDataSetChanged()
        binding.recycler.scheduleLayoutAnimation()      // Анимация обновления строк рециклера

    }

    private fun getRecords(specialMode: SpecialMode, idDir: Long, openDirMode: OpenDirMode)= lifecycleScope.launch {
        val records = when(specialMode) {
            SpecialMode.NORMAL, SpecialMode.MOVE, SpecialMode.DELETE -> {
                if (App.appSettings.sortingChecks) {
                    mainViewModel.getRecordsForNormalMoveDeleteModesByCheck(idDir)
                } else {
                    mainViewModel.getRecordsForNormalMoveDeleteModes(idDir)
                }
            }
            SpecialMode.RESTORE -> {
                if (App.appSettings.sortingChecks) {
                    mainViewModel.getRecordsForRestoreArchiveModesByCheck(idDir, 1)
                } else {
                    mainViewModel.getRecordsForRestoreArchiveModes(idDir, 1)
                }
            }
            SpecialMode.ARCHIVE -> {
                if (App.appSettings.sortingChecks) {
                    mainViewModel.getRecordsForRestoreArchiveModesByCheck(idDir, 0)
                } else {
                    mainViewModel.getRecordsForRestoreArchiveModes(idDir, 0)
                }
            }
        }
        val mutableRecords = records.toMutableList()
        if (specialMode == SpecialMode.NORMAL) {
            mutableRecords.add(getNewRecord(idDir, mutableRecords,mutableRecords.isEmpty() && App.appSettings.editEmptyDir))
        }
        fillingRecycler(mutableRecords, openDirMode)
    }

    private fun getNewRecord (idDir: Long, records: List<ListRecord>, startEdit: Boolean): ListRecord {
        return ListRecord(
            0,
            idDir,
            false,
            getMaxNpp(records) + 1,
            false,
            "",
            "",
            0,
            0,
            0,
            0,
            null,
            null,
            isArchive = false,
            isDelete = false,
            isFull = false,
            isAllCheck = false,
            true,
            startEdit,
            false
        )
    }

    private fun getMaxNpp (records: List<ListRecord>): Int {
        var maxNpp = 0
        for (rec: ListRecord in records) {
            if (rec.npp > maxNpp) maxNpp = rec.npp
        }
        return maxNpp
    }


    override fun getIdCurrentDir(): Long = idDir

    override fun insertNewRecord(record: ListRecord) = lifecycleScope.launch {
        val position = adapter.records.size - 1
        val id = mainViewModel.insertRecord(record)
        adapter.records[position].id = id
        adapter.notifyItemChanged(position)
    }

    override fun correctingPositionOfRecordByCheck (viewHolder: MainViewHolder) {
        if (App.appSettings.sortingChecks) {
            val fromPosition = adapter.records.indexOfFirst { it.id == viewHolder.id }
            val sortedRecords = ArrayList<ListRecord>()
            sortedRecords.addAll(adapter.records)
            val newRecord = sortedRecords.removeLast()
            sortedRecords.sortWith(compareBy(ListRecord::isChecked, ListRecord::npp))
            sortedRecords.add(newRecord)
            val toPosition = sortedRecords.indexOfFirst { it.id == viewHolder.id }
            if (fromPosition != toPosition) adapter.notifyItemMoved(fromPosition, toPosition)
            adapter.records.clear()
            adapter.records.addAll(sortedRecords)
            adapter.notifyItemRangeChanged(0, sortedRecords.size - 1)
        }
    }

    override fun updateRecord(record: ListRecord) {
        mainViewModel.updateRecord(record)
    }
}