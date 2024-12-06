package com.a2t.myapplication.main.ui

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.FrameLayout
import android.widget.LinearLayout
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
import com.a2t.myapplication.main.presentation.model.SpecialMode
import com.a2t.myapplication.root.ui.RootActivity
import com.a2t.myapplication.settings.presentation.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.hypot

const val K_MAX_SHIFT_RIGHT = 0.2f
const val K_MAX_SHIFT_LEFT = -0.3f
const val ANIMATION_DELEY = 300L
const val EYE_ANIMATION_DELEY = 5000L
// Что бы избежать инерции, будем выполнять изменение шрифта не каждый раз, а один раз
// на NUMBER_OF_OPERATIO_ZOOM срабатываний
const val NUMBER_OF_OPERATIO_ZOOM = 5
const val STEP_ZOOM = 0.5f                                     // Шаг изменения высоты шрифта

class MainFragment : Fragment(), MainAdapterCallback {
    private val mainViewModel by viewModel<MainViewModel>()
    private val settingsViewModel by viewModel<SettingsViewModel>()
    private val adapter = MainAdapter(this)
    private lateinit var recycler: RecyclerView
    private var mIth: ItemTouchHelper? = null
    private var mIthScb: ItemTouchHelper.Callback? = null
    private var nameDir = "R:"
    private lateinit var binding: FragmentMainBinding
    private lateinit var topToolbarBinding: ToolbarTopBinding
    private lateinit var smallToolbarBinding: ToolbarSmallBinding
    private lateinit var sideToolbarBinding: ToolbarSideBinding
    private lateinit var contextMenuFormatBinding: ContextMenuFormatBinding
    private lateinit var contextMenuMoveBinding: ContextMenuMoveBinding
    private lateinit var modesToolbarBinding: ToolbarModesBinding
    private var isZOOMode = false                                  // Режим ZOOM
    private var oldDist = 1f                                       // Расстояние между пальцами начальное
    private var newDist = 0f                                               // конечное, жест ZOOM
    private var sizeGrandText = 20f
    private var isSideToolbarFullShow = false
    private var widthScreen = 0                                    // Ширина экрана
    private var heightScreen = 0                                   // Ширина экрана
    private var maxShiftToRight = 0f                               // Величина максимального смещения при свайпе в право
    private var maxShiftToLeft = 0f                                // Величина максимального смещения при свайпе в лево
    private var hidhtContextMenu = 0                               // Высота контекстного меню
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
    private var nameJob = lifecycleScope.launch {}
    private var isNoSleepMode = false
    private var isClickAllowed = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        recycler = binding.recycler
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
        val dpSize = this.resources.displayMetrics.density              // Размер dp
        hidhtContextMenu = (56 * dpSize).toInt()                        // Высота контекстного меню в px
        maxShiftToRight = widthScreen * K_MAX_SHIFT_RIGHT               // Величина максимального смещения при свайпе в право
        maxShiftToLeft = widthScreen * K_MAX_SHIFT_LEFT                 // Величина максимального смещения при свайпе в лево
        sizeGrandText = App.appSettings.textSize
        topToolbarBinding.pathDir.textSize = 0.75f * sizeGrandText

        // Анимации
        animationMoveMode = AnimationUtils.loadAnimation(requireContext(), R.anim.arrow_right)
        animationDeleteMode = AnimationUtils.loadAnimation(requireContext(), R.anim.arrow_down)
        animationRestoreMode = AnimationUtils.loadAnimation(requireContext(), R.anim.arrow_up)
        animationArchiveMode = AnimationUtils.loadAnimation(requireContext(), R.anim.archive)
        animationEye = AnimationUtils.loadAnimation(requireContext(), R.anim.eye_eff)
        animOpenNewDir = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.anim_open_new_dir)
        animOpenChildDir = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.anim_open_child_dir)
        animOpenParentDir = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.anim_open_parent_dir)

        enableSpecialMode()
        initializingRecyclerView()

        goToDir(animOpenNewDir)

        // Изменение высоты шрифта
        val counter = AtomicInteger() // Счетчик срабатываний Zoom
        recycler.setOnTouchListener{ _: View?, event: MotionEvent ->
            when(event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    isZOOMode = false
                    }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    val dx = event.getX(0) - event.getX(1)
                    val dy = event.getY(0) - event.getY(1)
                    oldDist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    isZOOMode = true
                }

                MotionEvent.ACTION_MOVE -> {
                    counter.getAndIncrement()
                    if (counter.get() == NUMBER_OF_OPERATIO_ZOOM) {
                        counter.set(0) // Обнуляем счетчик
                        if (isZOOMode) {
                            val dx = event.getX(0) - event.getX(1)
                            val dy = event.getY(0) - event.getY(1)
                            newDist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                            val coef = newDist / oldDist
                            if (coef < 1f) {
                                sizeGrandText -= STEP_ZOOM
                                if (sizeGrandText < 18) sizeGrandText = 18f
                            } else if (coef > 1f) {
                                sizeGrandText += STEP_ZOOM
                                if (sizeGrandText > 27) sizeGrandText = 27f
                            }
                            App.appSettings.textSize = sizeGrandText
                            settingsViewModel.updateSettings()      // Сохраняем параметры
                            // Изменить размер шрифта в поле имя папки
                            topToolbarBinding.pathDir.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0.75f * sizeGrandText)
                            // Перерисовать recyclerView
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            isZOOMode
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ ГЛАВНАЯ ПАНЕЛЬ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

        // Кнопка МЕНЮ
        topToolbarBinding.btnMenu.setOnClickListener {
            requestMenuFocus()                   // Присвоение фокуса
            noSleepModeOff()           // Выключение режима БЕЗ СНА
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment2)
        }

        topToolbarBinding.pathDir.setOnClickListener {
            if (clickDebounce()) fullPathDir(getIdCurrentDir())
        }

        // НЕ СПЯЩИЙ РЕЖИМ
        topToolbarBinding.imageEye.setOnClickListener {
            requestMenuFocus()                   // Присвоение фокуса
            if (isNoSleepMode) noSleepModeOff() else noSleepModeON()

        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ БОКОВАЯ ПАНЕЛЬ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // swipe влево по флагу для открытия БОКОВОЙ ПАНЕЛИ
        val downX = AtomicReference( 0f)
        val downY = AtomicReference( 0f)
        val isTouch = AtomicBoolean(false)
        binding.sideBarContainer.setOnTouchListener { _, event ->
            when(event.action) {
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
                            noSleepModeOff()                                    // Выключение режима БЕЗ СНА
                            sideBarShowOrHide(true)                       // Открыть боковую панель
                        }
                    }
                }
            }
            return@setOnTouchListener isTouch.get()
        }
        // Потеря фокуса кнопкой развернуть/свернуть убирает бок.панель
        sideToolbarBinding.ivSideBarOpen.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) sideBarShowOrHide(false)
        }

        // Кнопка Развернуть/Свернуть боковую панель
        sideToolbarBinding.ivSideBarOpen.setOnClickListener {
            sideBarFullOpenClose()
        }
        sideToolbarBinding.tvSideBarOpen.setOnClickListener {
            sideBarFullOpenClose()
        }

        // Кнопка режима Переноса
        sideToolbarBinding.llSideBarMoveMode.setOnClickListener {
            requestFocusInTouch(view)
            mainViewModel.specialMode = SpecialMode.MOVE
            enableSpecialMode()
            goToDir(animOpenNewDir)
        }

        // Кнопка режима Удаления
        sideToolbarBinding.llSideBarDelMode.setOnClickListener {
            requestFocusInTouch(view)
            mainViewModel.specialMode = SpecialMode.DELETE
            enableSpecialMode()
            goToDir(animOpenNewDir)
        }

        // Кнопка режима Восстановления
        sideToolbarBinding.llSideBarRestMode.setOnClickListener {
            requestFocusInTouch(view)
            mainViewModel.specialMode = SpecialMode.RESTORE
            enableSpecialMode()
            goToDir(animOpenNewDir)
        }

        // Кнопка режима Архив
        sideToolbarBinding.llSideBarArchiveMode.setOnClickListener {
            requestFocusInTouch(view)
            mainViewModel.specialMode = SpecialMode.ARCHIVE
            enableSpecialMode()
            goToDir(animOpenNewDir)
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ МАЛАЯ ПАНЕЛЬ ИНСТРУМЕНТОВ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        smallToolbarBinding.llRootDir.setOnClickListener {
            if (getIdCurrentDir() != 0L) {
                mainViewModel.idDir = 0L
                goToDir(animOpenParentDir)
            }
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ НИЖНЯЯ ПАНЕЛЬ ИНСТРУМЕНТОВ РЕЖИМЫ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // Свайп вниз закрывает нижнюю панель и переводит рециклер в обычный режим
        modesToolbarBinding.clModesToolbar.setOnTouchListener { _, event ->
            when(event.action) {
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
                            completionSpecialMode()
                        }
                    }
                }
            }
            return@setOnTouchListener isTouch.get()
        }
        // Клик по кнопке Закрыть закрывает нижнюю панель и переводит экран в обычный режим
        modesToolbarBinding.btnClose.setOnClickListener {
            completionSpecialMode()
        }

        modesToolbarBinding.btnSelectAll.setOnClickListener {
            if (getSpecialMode() == SpecialMode.DELETE || getSpecialMode() == SpecialMode.RESTORE) {
                adapter.records.forEachIndexed { index, rec ->
                    if (!rec.isNew && getMainBuffer().all { it.id != rec.id }) {
                        getMainBuffer().add(rec)
                        adapter.notifyItemChanged(index)
                    }
                }
                showNumberOfSelectedRecords()
            }
        }

        modesToolbarBinding.btnAction.setOnClickListener {
            when(getSpecialMode()) {
                SpecialMode.MOVE -> {
                    if (getMainBuffer().size + getMoveBuffer().size > 0) {
                        val pasteIds = mutableListOf<Long>()
                        getMainBuffer().forEach { if (it.isDir) pasteIds.add(it.id) }
                        getMoveBuffer().forEach { if (it.isDir) pasteIds.add(it.id) }
                        mainViewModel.pasteRecords(getIdCurrentDir(), pasteIds,
                            {
                                showRecursionError()
                            },
                            {
                                pasteRecords()
                            }
                        )





                    } else {
                        Toast.makeText(requireContext(), getString(R.string.nothing_selected),Toast.LENGTH_SHORT).show()
                    }
                }
                SpecialMode.DELETE -> {
                    if (getMainBuffer().size > 0) {
                        deleteRecords(getMainBuffer())
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.nothing_selected),Toast.LENGTH_SHORT).show()
                    }
                }
                SpecialMode.RESTORE -> {
                    if (getMainBuffer().size > 0) {
                        restoreRecords(getMainBuffer())
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.nothing_selected),Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {}
            }
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ КОНТЕКСТНОЕ МЕНЮ ФОРМАТ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // Потеря фокуса контекст.меню приводит к скрытию меню
        contextMenuFormatBinding.llContextMenuFormat.setOnFocusChangeListener{ v: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                v?.isVisible = false
                adapter.currentHolderIdLiveData.postValue(0)
            }
        }
        contextMenuFormatBinding.btnTextColor1.setOnClickListener {



        }



        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ КОНТЕКСТНОЕ МЕНЮ MOVE $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // Потеря фокуса контекст.меню приводит к скрытию меню
        contextMenuMoveBinding.llContextMenuMove.setOnFocusChangeListener{ v: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                v?.isVisible = false
                adapter.currentHolderIdLiveData.postValue(0)
            }
        }
        // Кнопка ВЫРЕЗАТЬ
        contextMenuMoveBinding.btnCut.setOnClickListener {
            val item = adapter.currentItem
            getMainBuffer().removeAll { it.id == item?.id }
            getMoveBuffer().removeAll { it.id == item?.id }
            if (item != null) {
                getMoveBuffer().add(item)
                adapter.notifyItemChanged(adapter.currentHolderPosition)
                showNumberOfSelectedRecords()
            }
            requestFocusInTouch(view)
        }
        contextMenuMoveBinding.btnCut.setOnLongClickListener {
            adapter.records.forEachIndexed { index, item ->
                getMainBuffer().removeAll { it.id == item.id }
                getMoveBuffer().removeAll { it.id == item.id }
                getMoveBuffer().add(item)
                adapter.notifyItemChanged(index)
            }
            showNumberOfSelectedRecords()
            requestFocusInTouch(view)
            true
        }
        // Кнопка КОПИРОВАТЬ
        contextMenuMoveBinding.btnCopy.setOnClickListener {
            val item = adapter.currentItem
            getMainBuffer().removeAll { it.id == item?.id }
            getMoveBuffer().removeAll { it.id == item?.id }
            if (item != null) {
                getMainBuffer().add(item)
                adapter.notifyItemChanged(adapter.currentHolderPosition)
                showNumberOfSelectedRecords()
            }
            requestFocusInTouch(view)
        }
        contextMenuMoveBinding.btnCopy.setOnLongClickListener {
            adapter.records.forEachIndexed { index, item ->
                getMainBuffer().removeAll { it.id == item.id }
                getMoveBuffer().removeAll { it.id == item.id }
                getMainBuffer().add(item)
                adapter.notifyItemChanged(index)
            }
            showNumberOfSelectedRecords()
            requestFocusInTouch(view)
            true
        }
        // Кнопка ОТМЕНА
        contextMenuMoveBinding.btnBack.setOnClickListener {
            val item = adapter.currentItem
            getMainBuffer().removeAll { it.id == item?.id }
            getMoveBuffer().removeAll { it.id == item?.id }
            if (adapter.currentHolderPosition > 0) {
                adapter.notifyItemChanged(adapter.currentHolderPosition)
                showNumberOfSelectedRecords()
            }
            requestFocusInTouch(view)
        }
        contextMenuMoveBinding.btnBack.setOnLongClickListener {
            adapter.records.forEachIndexed { index, item ->
                getMainBuffer().removeAll { it.id == item.id }
                getMoveBuffer().removeAll { it.id == item.id }
                adapter.notifyItemChanged(index)
            }
            showNumberOfSelectedRecords()
            requestFocusInTouch(view)
            true
        }
    }

    // Режим БЕЗ СНА
    // Включение режима БЕЗ СНА
    private fun noSleepModeON() {
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
    private fun noSleepModeOff() {
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
    override fun requestMenuFocus() {
        requestFocusInTouch(topToolbarBinding.btnMenu)
    }

    private fun requestFocusInTouch(view: View) {
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.isFocusableInTouchMode = false
    }


    // Открытие специального режима
    private fun enableSpecialMode() {
        noSleepModeOff()           // Выключение режима БЕЗ СНА
        topToolbarBinding.imageEye.isVisible = getSpecialMode() == SpecialMode.NORMAL
        showSpecialModeToolbar()
        showNumberOfSelectedRecords()
    }

    // Показать панель инструментов специального режима
    private fun showSpecialModeToolbar() {
        stopAllModeAnimations()         // Остановить все анимации
        modesToolbarBinding.clModesToolbar.isVisible = getSpecialMode() != SpecialMode.NORMAL
        when(getSpecialMode()) {
            SpecialMode.MOVE -> {
                modesToolbarBinding.btnSelectAll.isVisible = false
                modesToolbarBinding.btnAction.isVisible = true
                modesToolbarBinding.btnAction.text = getString(R.string.insert)
                modesToolbarBinding.tvModeHint.isVisible = false
            }
            SpecialMode.DELETE -> {
                modesToolbarBinding.btnSelectAll.isVisible = true
                modesToolbarBinding.btnAction.isVisible = true
                modesToolbarBinding.btnAction.text = getString(R.string.delete)
                modesToolbarBinding.tvModeHint.isVisible = true
            }
            SpecialMode.RESTORE -> {
                modesToolbarBinding.btnSelectAll.isVisible = true
                modesToolbarBinding.btnAction.isVisible = true
                modesToolbarBinding.btnAction.text = getString(R.string.restore)
                modesToolbarBinding.tvModeHint.isVisible = true
            }
            SpecialMode.ARCHIVE -> {
                modesToolbarBinding.btnSelectAll.isVisible = false
                modesToolbarBinding.btnAction.isVisible = false
                modesToolbarBinding.tvModeHint.isVisible = false
            }
            SpecialMode.NORMAL -> {}
        }
        showIconMode()

    }

    private fun showIconMode() {
        when(getSpecialMode()) {
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
    private suspend fun archiveModeAnimation() {
        delay(ANIMATION_DELEY)
        modesToolbarBinding.ivBarModes2.startAnimation(animationArchiveMode)            // Анимация
        delay(ANIMATION_DELEY)
        modesToolbarBinding.ivBarModes3.startAnimation(animationArchiveMode)            // Анимация
        delay(ANIMATION_DELEY)
        modesToolbarBinding.ivBarModes1.startAnimation(animationArchiveMode)            // Анимация
        archiveModeAnimation()                                                              // Рекурсия
    }

    // Остановить все анимации
    private fun stopAllModeAnimations() {
        archiveJob.cancel()
        modesToolbarBinding.ivBarModes1.clearAnimation()
        modesToolbarBinding.ivBarModes2.clearAnimation()
        modesToolbarBinding.ivBarModes3.clearAnimation()
    }

    // Показать/скрыть боковую панель
    private fun sideBarShowOrHide(show: Boolean) {
        if (show) {
            binding.sideBarFlag.isVisible = false                           // Убрать ярлык боковой панели
            sideToolbarBinding.llSideBar.isVisible = true                   // Вывести бок.панель
            sideToolbarBinding.ivSideBarOpen.requestFocus()                 // и перевести фокус на нее
            sideToolbarBinding.ivSideBarOpen.animate().rotation(0f)   // Кнопку Развернуть панель в исх.положение
        } else {
            if (isSideToolbarFullShow) sideBarFullOpenClose()      // Свернуть бок.панель
            sideToolbarBinding.llSideBar.isVisible = false                  // Убрать бок.панель
            lifecycleScope.launch {
                delay(150)
                binding.sideBarFlag.isVisible = true
            }
        }
    }

    // Разворачивание/сворачивание боковой панели
    private fun sideBarFullOpenClose() {
        if (isSideToolbarFullShow) {
            sideToolbarBinding.ivSideBarOpen.animate().rotation(0f)       // Перевернуть кнопку Развернуть панель
            showSideBarText(false)                                        // Убрать пояснительный текст кнопок
        } else {
            sideToolbarBinding.ivSideBarOpen.animate().rotation(180f)     // Перевернуть кнопку Развернуть панель
            showSideBarText(true)                                         // Показать пояснительный текст кнопок
        }
        isSideToolbarFullShow = !isSideToolbarFullShow
    }

    // Показать пояснительный текст кнопок боковой панели
    private fun showSideBarText(show: Boolean) {
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

    private fun getItemById(id: Long, records: List<ListRecord>): ListRecord? {
        return records.find { it.id == id }
    }

    private fun updateNppList(list: ArrayList<ListRecord>) {
        list.forEachIndexed { index, item -> item.npp = index }
        mainViewModel.updateRecords(list) {}// Записать изменения в БД
    }
    private fun initializingRecyclerView() {
        recycler.adapter = adapter
        recycler.setLayoutManager(object : LinearLayoutManager(requireContext()) {
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
        recycler.setItemAnimator(itemAnimator)
        recycler.scheduleLayoutAnimation()
        recycler.layoutAnimation = animOpenNewDir
        recycler.invalidate()
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
                return getSpecialMode() == SpecialMode.NORMAL            // Swipe будет только в нормальном режиме
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
                    if (!item.isNew  && getSpecialMode() == SpecialMode.NORMAL) {
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
                    updateNppList(listDir) // Обновить порядковые номера записей в массиве и БД
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
        mIth!!.attachToRecyclerView(recycler)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mIth!!.startDrag(viewHolder)
    }

    // Вызывается из адаптера для возврата Foreground в исходное положение
    override fun returnHolderToOriginalState(viewHolder: RecyclerView.ViewHolder) {
        val foregroundView = (viewHolder as MainViewHolder).llForeground
        ItemTouchHelper.Callback.getDefaultUIUtil().clearView(foregroundView)
    }

    fun mainBackPressed() {
        adapter.isKeyboardON = false            // Если нажат Back, клавиатура точно скрыта
        requestMenuFocus()
        noSleepModeOff()                        // Выключение режима БЕЗ СНА
        goToParentDir()                        // Переход к родительской папке
    }

    override fun completionSpecialMode() {
        getMainBuffer().clear()
        getMoveBuffer().clear()
        goToNormalMode()
    }

    // Возврат в режим NORMAL
    private fun goToNormalMode() {
        requestMenuFocus()
        mainViewModel.specialMode = SpecialMode.NORMAL
        enableSpecialMode()
        goToDir(animOpenNewDir)
    }

    private fun goToParentDir() {
        mainViewModel.getParentDirId(getIdCurrentDir()) { ids ->
            mainViewModel.idDir = ids[0]
            goToDir(animOpenParentDir)
        }
    }

    override fun goToChildDir(id: Long) {
        mainViewModel.idDir = id
        goToDir(animOpenChildDir)
    }

    private fun goToDir(animationController: LayoutAnimationController) {
        binding.progressBar.isVisible = true
        mainViewModel.deletingExpiredRecords {
            showList(animationController)
        }
    }

    private fun showList(animationController: LayoutAnimationController) = lifecycleScope.launch {
        mainViewModel.getRecords { records ->
            fillingRecycler(records, animationController)
            updatFieldsOfSmallToolbar()
            binding.progressBar.isVisible = false
        }

    }

    private fun fillingRecycler(records: List<ListRecord>, animationController: LayoutAnimationController) {
        recycler.layoutAnimation = animationController
        mainViewModel.getNameDir(getIdCurrentDir()) { names ->
            nameDir = if (names.isEmpty()) "R:" else names[0]
            topToolbarBinding.pathDir.text = nameDir
        }
        adapter.specialMode = getSpecialMode()
        adapter.records.clear()
        adapter.records.addAll(records)
        adapter.notifyDataSetChanged()
        recycler.scheduleLayoutAnimation()      // Анимация обновления строк рециклера
    }

    override fun insertNewRecord(item: ListRecord) {
        mainViewModel.insertRecord(item) { id ->
            item.id = id
        }

    }

    override fun correctingPositionOfRecordByCheck(viewHolder: MainViewHolder) {
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
        mainViewModel.updateRecord(record) {}
    }

    override fun showContextMenuFormat(viewHolder: MainViewHolder) {
        showContextMenu(viewHolder, contextMenuFormatBinding.llContextMenuFormat)
    }

    override fun showContextMenuMove(viewHolder: MainViewHolder) {
        showContextMenu(viewHolder, contextMenuMoveBinding.llContextMenuMove)
    }

    private fun showContextMenu(viewHolder: MainViewHolder, contextMenu: LinearLayout) {
        // Передвигаем контекст.меню в нужную точку
        val params = contextMenu.layoutParams as FrameLayout.LayoutParams
        params.topMargin = getYContextMenu(viewHolder)
        contextMenu.layoutParams = params
        contextMenu.isVisible = true
        contextMenu.requestFocus()
    }
    // По скольку метод getLocationOnScreen возвращает значение Y в системе координат с началом отсчета
    // в верхнем левом углу экрана, а контекстное меню вставляется в контейнер с началом отсчета
    // в верхнем левом углу верхней панели инструментов, вводим коррекцию
    private fun getYContextMenu(viewHolder: MainViewHolder): Int {
        val location = IntArray(2)
        topToolbarBinding.llTopToolbar.getLocationOnScreen(location)
        val hStatusBar = location[1]            // Высота строки состояния
        val borderY = heightScreen * 2 / 3      // Граница:
                                                // для холдеров находящихся НАД ней контекст.меню выводится ПОД холдером,
                                                // для холдеров находящихся ПОД граеницей - НАД холдером
        viewHolder.llForeground.getLocationOnScreen(location)
        val holderTopY = location[1] - hStatusBar       // Y верхнего угла холдера с коррекцией на высоту строки состояния
        val holderBottomY =
            holderTopY + viewHolder.llForeground.height // Y нижнего угла холдера
        val coordinateY = if (holderTopY < borderY) {   // Если холдер находится над границей
            holderBottomY + 20                          // Контекст.меню выводится на 20 пикселов ниже холдера
        } else {
            holderTopY - 20 - hidhtContextMenu          // Контекст.меню выводится на 20 пикселов выше холдера
        }
        return coordinateY                              // Y точки, в которую надо вывести контекст.меню
    }

    override fun updatFieldsOfSmallToolbar() {
        var countLine = 0
        var countDir = 0
        adapter.records.forEach {
            if (it.isDir) countDir++ else countLine++
        }
        if (getSpecialMode() == SpecialMode.NORMAL) countLine--
        val sum = (countDir + countLine).toString()
        smallToolbarBinding.tvSumLine.text = countLine.toString()
        smallToolbarBinding.tvSumDir.text = countDir.toString()
        smallToolbarBinding.tvSumSum.text = sum
    }

    private fun fullPathDir(idDir: Long) {
        var id = idDir
        if (id > 0) {
            mainViewModel.getParentDirId(id) { ids ->
                id = ids[0]
                mainViewModel.getNameDir(id) { names ->
                    val name = if (names.isEmpty()) "R:" else names[0]
                    topToolbarBinding.pathDir.text = buildString {
                        append(name)
                        append("\\")
                        append(topToolbarBinding.pathDir.text)
                    }
                    fullPathDir(id)
                    nameJob.cancel()
                    nameJob = lifecycleScope.launch {
                        delay(5000)
                        topToolbarBinding.pathDir.text = nameDir
                    }
                }

            }
        }
    }

    private fun clickDebounce(): Boolean {
        val current = isClickAllowed
        if (isClickAllowed) {
            isClickAllowed = false
            viewLifecycleOwner.lifecycleScope.launch {
                delay(5100)
                isClickAllowed = true
            }
        }
        return current
    }

    @SuppressLint("InflateParams")
    override fun deleteRecords(records: List<ListRecord>) {
        mainViewModel.selectionSubordinateRecordsToDelete(records) { list ->
            val mutableRecords = list.toMutableList()
            val selectedRecords = records.size
            val subordinateRecords = mutableRecords.size
            val countArchive = mutableRecords.count { it.isArchive }
            mutableRecords.addAll(records)
            var mess = getString(R.string.del_attempt, selectedRecords.toString())
            var str = if (subordinateRecords != 0) getString(R.string.del_subordinate, subordinateRecords.toString()) else ""
            mess += str
            str = if (countArchive != 0) getString(R.string.del_archive, countArchive.toString()) else ""
            mess += "$str."
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_title_attention, null)
            MaterialAlertDialogBuilder(requireContext())
                .setCustomTitle(dialogView)
                .setMessage(mess)
                .setNeutralButton(getString(R.string.negative_btn)) { _, _ -> }
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    mutableRecords.forEach { it.isDelete = true }
                    mainViewModel.updateRecords(mutableRecords) {
                        if (getSpecialMode() == SpecialMode.NORMAL) {
                            val position = adapter.records.indexOfFirst { it.id == records[0].id }
                            adapter.records.removeAt(position)
                            adapter.notifyItemRemoved(position) // Уведомление об удалении
                            adapter.notifyItemRangeChanged(
                                position,
                                adapter.records.size - position
                            )
                        } else {
                            completionSpecialMode()
                        }
                    }
                }
                .show()
        }
    }

    @SuppressLint("InflateParams")
    fun restoreRecords(records: List<ListRecord>) {
        mainViewModel.selectionSubordinateRecordsToRestore(records) { list ->
            val mutableRecords = list.toMutableList()
            val selectedRecords = records.size
            val subordinateRecords = mutableRecords.size
            val countArchive = mutableRecords.count { it.isArchive }
            mutableRecords.addAll(records)
            var mess = getString(R.string.rest_attempt, selectedRecords.toString())
            var str = if (subordinateRecords != 0) getString(R.string.rest_subordinate, subordinateRecords.toString()) else ""
            mess += str
            str = if (countArchive != 0) getString(R.string.del_archive, countArchive.toString()) else ""
            mess += "$str."
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_title_attention, null)
            MaterialAlertDialogBuilder(requireContext())
                .setCustomTitle(dialogView)
                .setMessage(mess)
                .setNeutralButton(getString(R.string.negative_btn)) { _, _ -> }
                .setPositiveButton(getString(R.string.restore)) { _, _ ->
                    mutableRecords.forEach { it.isDelete = false }
                    mainViewModel.updateRecords(mutableRecords) {
                        if (getSpecialMode() == SpecialMode.RESTORE) completionSpecialMode()
                    }
                }
                .show()
        }
    }

    private fun pasteRecords() {
        // Перенос записей
        getMoveBuffer().forEach { it.idDir = getIdCurrentDir() }
        mainViewModel.updateRecords(getMoveBuffer()) {
            // Копирование
            mainViewModel.copyRecords(getMainBuffer()){
                completionSpecialMode()
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showRecursionError() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_title_error, null)
        MaterialAlertDialogBuilder(requireContext())
            .setCustomTitle(dialogView)
            .setMessage(getString(R.string.recursion_error))
            .setPositiveButton(getString(R.string.ok)) { _, _ -> }
            .show()
        completionSpecialMode()
    }

    fun getSpecialMode(): SpecialMode = mainViewModel.specialMode

    override fun getIdCurrentDir(): Long = mainViewModel.idDir

    override fun getMoveBuffer(): ArrayList<ListRecord> = mainViewModel.moveBuffer

    override fun getMainBuffer(): ArrayList<ListRecord> = mainViewModel.mainBuffer

    override fun showNumberOfSelectedRecords() {
        val number = getMainBuffer().size + getMoveBuffer().size
        modesToolbarBinding.countRecords.text = number.toString()
        modesToolbarBinding.countRecords.isVisible = number > 0
    }
}