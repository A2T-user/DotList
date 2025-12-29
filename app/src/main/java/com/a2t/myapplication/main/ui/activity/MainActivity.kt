package com.a2t.myapplication.main.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.LayoutAnimationController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.common.App
import com.a2t.myapplication.R
import com.a2t.myapplication.common.model.DLAnimator
import com.a2t.myapplication.databinding.ActivityMainBinding
import com.a2t.myapplication.databinding.ContextMenuFormatBinding
import com.a2t.myapplication.databinding.ContextMenuMoveBinding
import com.a2t.myapplication.databinding.ToolbarModesBinding
import com.a2t.myapplication.databinding.ToolbarSmallBinding
import com.a2t.myapplication.databinding.ToolbarTopBinding
import com.a2t.myapplication.description.ui.DescriptionActivity
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.presentation.MainViewModel
import com.a2t.myapplication.main.ui.activity.managers.ContextMenuFormatManager
import com.a2t.myapplication.main.ui.activity.managers.ContextMenuMoveManager
import com.a2t.myapplication.main.ui.activity.managers.ModesToolbarManager
import com.a2t.myapplication.main.ui.activity.model.SpecialMode
import com.a2t.myapplication.main.ui.activity.recycler.CustomizerRecyclerView
import com.a2t.myapplication.main.ui.activity.recycler.MainAdapter
import com.a2t.myapplication.main.ui.activity.recycler.MainAdapterCallback
import com.a2t.myapplication.main.ui.activity.recycler.MainViewHolder
import com.a2t.myapplication.main.ui.activity.recycler.MyScrollListener
import com.a2t.myapplication.main.ui.activity.recycler.OnScrollStateChangedListener
import com.a2t.myapplication.main.ui.activity.recycler.model.ScrollState
import com.a2t.myapplication.main.ui.fragments.MainMenuFragment
import com.a2t.myapplication.main.ui.fragments.ToolbarSideFragment
import com.a2t.myapplication.main.ui.fragments.ToolbarSideFragment.Companion.SWIPE_THRESHOLD
import com.a2t.myapplication.utilities.AlarmHelper
import com.a2t.myapplication.utilities.AppHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.internal.ViewUtils.dpToPx
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs
import kotlin.math.hypot


const val ANIMATION_DELEY = 300L
const val EYE_ANIMATION_DELEY = 5000L
// Что бы избежать инерции, будем выполнять изменение шрифта не каждый раз, а один раз
// на NUMBER_OF_OPERATIO_ZOOM срабатываний TouchListener
const val NUMBER_OF_OPERATIO_ZOOM = 5
const val STEP_ZOOM = 0.3f                                     // Шаг изменения размера шрифта
const val CURRENT_TAB = "current_tab"
private const val HIDE_CONTEXT_MENU_DEBOUNCE_DELAY = 3000L                 // Задержка закрытия контекстного меню

class MainActivity: AppCompatActivity(), MainAdapterCallback, OnScrollStateChangedListener {
    lateinit var mainBackPressedCallback: OnBackPressedCallback
    private lateinit var floatingBarBackPressedCallback: OnBackPressedCallback
    val fragmentManager: FragmentManager = supportFragmentManager
    private val mainViewModel: MainViewModel by viewModel()
    val adapter = MainAdapter(this)
    private lateinit var recycler: RecyclerView
    private var mIth: ItemTouchHelper? = null
    private lateinit var dlAnimator: DLAnimator
    private var nameDir = "R:"
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var topToolbarBinding: ToolbarTopBinding
    private lateinit var smallToolbarBinding: ToolbarSmallBinding
    private lateinit var contextMenuFormatBinding: ContextMenuFormatBinding
    lateinit var contextMenuMoveBinding: ContextMenuMoveBinding
    private lateinit var modesToolbarBinding: ToolbarModesBinding
    private var oldDist = 1f                                        // Расстояние между пальцами начальное
    private var newDist = 0f                                        // конечное, жест ZOOM
    private var sizeGrandText = 20f
    var widthScreen = 0                                             // Ширина экрана
    private var heightScreen = 0                                   // Ширина экрана
    private var heighContextMenu = 0                               // Высота контекстного меню
    private var archiveJob = lifecycleScope.launch {}
    private var eyeJob = lifecycleScope.launch {}
    private var nameJob = lifecycleScope.launch {}
    private var scrollJob = lifecycleScope.launch {}
    private var isNoSleepMode = false
    private var isClickAllowed = true
    private var isSideBarOpenAllowed = true
    private var scrollState = ScrollState.STOPPED
    private lateinit var velocityTracker: VelocityTracker
    private lateinit var specialModeGestureDetector: GestureDetector
    private var isLeftHandControl: Boolean = false
    private var hideContextMenuJob: Job? = null
    private var isBackPressedOnce = false // Флаг для отслеживания нажатия Back

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        recycler = binding.recycler
        topToolbarBinding = binding.topToolbar
        smallToolbarBinding = binding.smallToolbar
        contextMenuFormatBinding = binding.contextMenuFormat
        contextMenuMoveBinding = binding.contextMenuMove
        modesToolbarBinding = binding.modesToolbar

        setContentView(binding.root)

        // Статус-бар и навигационный бар не накладываются на контент UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {  // API 35+
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        installHandControl()

        val app = applicationContext as App
        // Необходимые размеры
        // Определяем ширину экрана, пределы смещения холдера вдоль оси Х вправо и влево
        widthScreen = resources.displayMetrics.widthPixels
        heightScreen = resources.displayMetrics.heightPixels
        val dpSize = resources.displayMetrics.density              // Размер dp
        heighContextMenu = (58 * dpSize).toInt()                        // Высота контекстного меню в px
        App.getTextSizeLiveData().observe(this) { size ->
            sizeGrandText = size
            topToolbarBinding.pathDir.textSize = 0.75f * sizeGrandText
        }

        // Анимации
        dlAnimator = DLAnimator()

        // $$$$$$$$$$$$$$$$$$$$$$   Реакция на нажатие системной кнопки BACK   $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        mainBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mainBackPressedCallback.isEnabled = true
                requestMenuFocus()
                when (getSpecialMode()) {
                    SpecialMode.DELETE, SpecialMode.RESTORE -> {}
                    else -> {
                        if (getIdCurrentDir() > 0) {
                            normBackPressed()
                        } else {                                    // Выход по двойному нажатию Back
                            if (isBackPressedOnce) {
                                finish()                            // Завершаем активность
                            } else {
                                isBackPressedOnce = true            // Устанавливаем флаг
                                Toast.makeText(this@MainActivity, R.string.text_exit, Toast.LENGTH_SHORT).show()
                                // Запускаем корутину для сброса флага через 2 секунды
                                lifecycleScope.launch {
                                    delay(2000)
                                    isBackPressedOnce = false       // Сбрасываем флаг
                                }
                            }
                        }
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, mainBackPressedCallback)

        floatingBarBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requestMenuFocus()
            }
        }
        floatingBarBackPressedCallback.isEnabled = false
        onBackPressedDispatcher.addCallback(this, floatingBarBackPressedCallback)

        val idDirInt = intent.getLongExtra("IDDIR", -1L)
        if (idDirInt != -1L) mainViewModel.idDir = idDirInt

        val startDay = AlarmHelper.startOfCurrentDay()
        mainViewModel.deleteOldAlarm(startDay) {
            binding.progressBar.isVisible = true
            enableSpecialMode()
            initializingRecyclerView()
            goToDir(dlAnimator.animOpenNewDir)
        }
        deleteOldAlarm(startDay)

        if (App.appSettings.launchCounter == 1) {
            val dialogView =
                LayoutInflater.from(this).inflate(R.layout.dialog_title_attention, null)
            MaterialAlertDialogBuilder(this)
                .setCustomTitle(dialogView)
                .setMessage(getString(R.string.first_launch_message))
                .setNeutralButton(getString(R.string.first_launch_neutral_button)) { _, _ -> }
                .setPositiveButton(getString(R.string.first_launch_positive_button)) { _, _ ->
                    val intent = Intent(this, DescriptionActivity::class.java)
                    startActivity(intent)
                }
                .show()
        }

        // Изменение высоты шрифта
        var isZoom = false                                  // Режим ZOOM
        var counter = 0 // Счетчик срабатываний Zoom
        recycler.setOnTouchListener{ _: View?, event: MotionEvent ->
            requestMenuFocus()
            when(event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    isZoom = false
                }
                MotionEvent.ACTION_UP -> {
                    isZoom = false
                    app.saveTextSize()
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val dx = event.getX(0) - event.getX(1)
                    val dy = event.getY(0) - event.getY(1)
                    oldDist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    isZoom = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount >= 2) {
                        counter++
                        if (counter % NUMBER_OF_OPERATIO_ZOOM == 0) {
                            if (isZoom) {
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
                                app.setTextSize(sizeGrandText)
                                oldDist = newDist
                            }
                        }
                    }
                }
            }
            isZoom
        }

        // ПРОКРУТКА
        recycler.addOnScrollListener(MyScrollListener(this))

        binding.ivBtnScroll.setOnClickListener {
            when(scrollState) {
                ScrollState.DOWN -> recycler.smoothScrollToPosition(adapter.itemCount - 1)
                ScrollState.UP -> recycler.smoothScrollToPosition(0)
                else -> {}
            }
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ ГЛАВНАЯ ПАНЕЛЬ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // Кнопка МЕНЮ
        topToolbarBinding.btnMenu.setOnClickListener {
            requestMenuFocus()
            fragmentManager.beginTransaction().setTransition(TRANSIT_FRAGMENT_OPEN)
                .add(R.id.container_menu, MainMenuFragment())
                .addToBackStack("MainMenuFragment").commit()
        }

        topToolbarBinding.pathDir.setOnClickListener {
            requestMenuFocus()
            if (clickDebounce()) fullPathDir(getIdCurrentDir())
        }

        topToolbarBinding.ivEye.setOnClickListener {
            noSleepModeOff()
        }
        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ БОКОВАЯ ПАНЕЛЬ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // swipe влево по флагу для открытия БОКОВОЙ ПАНЕЛИ
        var downX = 0f
        var downY = 0f
        var isTouch = false
        binding.sideBarContainer.setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    isTouch = true
                }
                MotionEvent.ACTION_CANCEL -> isTouch = false
                MotionEvent.ACTION_UP, MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_MOVE -> {
                    if (isTouch) {
                        var dX = event.x - downX
                        val dY = event.y - downY
                        if (isLeftHandControl) dX *= -1
                        if (abs(dX / dY) > 1.5 && dX < 0) {         // Если жест горизонталный, влево
                            if (sideBarDebounce()) {
                                noSleepModeOff()                           // Выключение режима БЕЗ СНА
                                sideBarShow()                              // Открыть боковую панель
                            }
                        }
                    }
                }
            }
            return@setOnTouchListener isTouch
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ МАЛАЯ ПАНЕЛЬ ИНСТРУМЕНТОВ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        smallToolbarBinding.llRootDir.setOnClickListener {
            requestMenuFocus()
            if (getSpecialMode() != SpecialMode.DELETE && getSpecialMode() != SpecialMode.RESTORE) {
                if (getIdCurrentDir() != 0L) {
                    mainViewModel.idDir = 0L
                    goToDir(dlAnimator.animOpenParentDir)
                }
            }
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ НИЖНЯЯ ПАНЕЛЬ ИНСТРУМЕНТОВ РЕЖИМЫ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // Получаем список View панели
        val btnsModesToolbar = listOf(
            modesToolbarBinding.root,
            modesToolbarBinding.btnHelp,
            modesToolbarBinding.btnCloseToolbar,
            modesToolbarBinding.btnSelectAll,
            modesToolbarBinding.btnAction
        )
        val modesToolbarManager = ModesToolbarManager(this, mainViewModel)
        // Каждой View панели присваиваем слушателя
        var isSwipe = false
        for (btn in btnsModesToolbar) {
            btn.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                        isSwipe = false
                    }
                    MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_MOVE -> {
                        var dX = event.x - downX
                        val dY = event.y - downY
                        if (App.appSettings.isLeftHandControl) dX *= -1
                        if (abs(dY / dX) > 1 && dY > SWIPE_THRESHOLD) {         // Если жест горизонталный, влево
                            isSwipe = true
                            if (sideBarDebounce()) {
                                completionSpecialMode()
                            }
                        }

                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isSwipe) modesToolbarManager.clickBtn(btn.id)
                    }
                }
                return@setOnTouchListener true
            }
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ КОНТЕКСТНОЕ МЕНЮ ФОРМАТ $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        val contextMenuFormatManager = ContextMenuFormatManager(this, adapter, mainViewModel)
        // Потеря фокуса контекст.меню приводит к скрытию меню
        contextMenuFormatBinding.llContextMenuFormat.setOnFocusChangeListener{ v: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                dlAnimator.animationHideAlpha(v!!, 500)
                cancelCurrentHolder()
                mainBackPressedCallback.isEnabled = true
                floatingBarBackPressedCallback.isEnabled = false
            }
        }

        // Получаем список кнопок панели
        val btnMenuFormat = listOf(
            contextMenuFormatBinding.btnTextColor1,
            contextMenuFormatBinding.btnTextColor2,
            contextMenuFormatBinding.btnTextColor3,
            contextMenuFormatBinding.btnTextStyleB,
            contextMenuFormatBinding.btnTextStyleI,
            contextMenuFormatBinding.btnTextStyleBI,
            contextMenuFormatBinding.btnTextStyleU,
            contextMenuFormatBinding.btnTextRegular
        )

        // Каждой кнопке панели присваиваем слушателей
        for (btn in btnMenuFormat) {
            btn.setOnClickListener {
                contextMenuFormatManager.clickBtn(btn.id)
            }
            btn.setOnLongClickListener {
                contextMenuFormatManager.longClickBtn(btn.id)
                true
            }
        }

        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ КОНТЕКСТНОЕ МЕНЮ MOVE $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        val contextMenuMoveManager = ContextMenuMoveManager(this, adapter, mainViewModel)
        // Потеря фокуса контекст.меню приводит к скрытию меню
        contextMenuMoveBinding.llContextMenuMove.setOnFocusChangeListener{ v: View?, hasFocus: Boolean ->
            if (!hasFocus) {
                dlAnimator.animationHideAlpha(v!!, 500)
                cancelCurrentHolder()
                mainBackPressedCallback.isEnabled = true
                floatingBarBackPressedCallback.isEnabled = false
            }
        }

        // Получаем список кнопок панели
        val btnMenuMove = listOf(contextMenuMoveBinding.btnCut, contextMenuMoveBinding.btnCopy, contextMenuMoveBinding.btnBack)

        // Каждой кнопке панели присваиваем слушателей
        for (btn in btnMenuMove) {
            btn.setOnClickListener {
                contextMenuMoveManager.clickBtn(btn.id)
            }
            btn.setOnLongClickListener {
                contextMenuMoveManager.longClickBtn(btn.id)
                true
            }
        }
    }

    // Режим БЕЗ СНА
    // Включение режима БЕЗ СНА
    fun noSleepModeON() {
        isNoSleepMode = true
        switchNoSleepMode(true)    // В активити включаем keepScreenOn
        topToolbarBinding.ivEye.isVisible = true
        topToolbarBinding.ivEye.startAnimation(dlAnimator.animationEye)             // Анимация иконки
        // Маргание иконки каждые 10 секунд
        eyeJob = lifecycleScope.launch {
            eyeAnimation()
        }
    }
    // Выключение режима БЕЗ СНА
    private fun noSleepModeOff() {
        isNoSleepMode = false
        switchNoSleepMode(false)       // В активити выключаем keepScreenOn
        topToolbarBinding.ivEye.isVisible = false
        eyeJob.cancel()                                                         // Отменить маргание глаза
    }
    // Анимация глаза
    private suspend fun eyeAnimation() {
        delay(EYE_ANIMATION_DELEY)
        topToolbarBinding.ivEye.startAnimation(dlAnimator.animationEye)             // Анимация иконки
        eyeAnimation()                                                      // Рекурсия
    }

    // Включение/выключение режима БЕЗ СНА
    private fun switchNoSleepMode(isOn: Boolean) {
        binding.mainLayout.keepScreenOn = isOn
    }

    // Присвоение фокуса кнопке меню
    override fun requestMenuFocus() {
        AppHelper.requestFocusInTouch(topToolbarBinding.btnMenu)
    }

    // Показать скрыть контейнер и флажок бокового меню
    fun showSideBarContainer (isShow: Boolean) {
        binding.sideBarContainer.isVisible = isShow
    }

    // Открытие специального режима
    override fun enableSpecialMode() {
        noSleepModeOff()           // Выключение режима БЕЗ СНА
        showSideBarContainer (getSpecialMode() == SpecialMode.NORMAL)
        showSpecialModeToolbar()
        showNumberOfSelectedRecords()
    }

    override fun enableSpecialMode(mode: SpecialMode) {
        mainViewModel.specialMode = mode
        enableSpecialMode()
        goToDir(dlAnimator.animOpenNewDir)
    }

    private fun enableSelectAllButtons (mode: SpecialMode, records: List<ListRecord>) {
        if (mode == SpecialMode.DELETE || mode == SpecialMode.RESTORE) {
            val switchOn = records.isNotEmpty()
            modesToolbarBinding.btnSelectAll.isEnabled = switchOn
            modesToolbarBinding.btnSelectAll.alpha = if (switchOn) 1.0f else 0.3f
        }

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

    // Иконка панели инструментов специального режима
    private fun showIconMode() {
        when(getSpecialMode()) {
            SpecialMode.MOVE -> {
                modesToolbarBinding.ivBarModes3.isVisible = false
                modesToolbarBinding.ivBarModes2.setImageResource(R.drawable.ic_move_mode_2)
                modesToolbarBinding.ivBarModes1.setImageResource(R.drawable.ic_move_mode_1)
                modesToolbarBinding.ivBarModes1.startAnimation(dlAnimator.animationMoveMode)               // Анимация
            }
            SpecialMode.DELETE -> {
                modesToolbarBinding.ivBarModes3.isVisible = false
                modesToolbarBinding.ivBarModes2.setImageResource(R.drawable.ic_basket)
                modesToolbarBinding.ivBarModes1.setImageResource(R.drawable.ic_arrow_red)
                modesToolbarBinding.ivBarModes1.startAnimation(dlAnimator.animationDeleteMode)             // Анимация
            }
            SpecialMode.RESTORE -> {
                modesToolbarBinding.ivBarModes3.isVisible = false
                modesToolbarBinding.ivBarModes2.setImageResource(R.drawable.ic_basket)
                modesToolbarBinding.ivBarModes1.setImageResource(R.drawable.ic_arrow_blue)
                modesToolbarBinding.ivBarModes1.startAnimation(dlAnimator.animationRestoreMode)            // Анимация
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
        modesToolbarBinding.ivBarModes2.startAnimation(dlAnimator.animationArchiveMode)            // Анимация
        delay(ANIMATION_DELEY)
        modesToolbarBinding.ivBarModes3.startAnimation(dlAnimator.animationArchiveMode)            // Анимация
        delay(ANIMATION_DELEY)
        modesToolbarBinding.ivBarModes1.startAnimation(dlAnimator.animationArchiveMode)            // Анимация
        archiveModeAnimation()                                                              // Рекурсия
    }

    // Остановить все анимации иконки панели режимов
    private fun stopAllModeAnimations() {
        archiveJob.cancel()
        modesToolbarBinding.ivBarModes1.clearAnimation()
        modesToolbarBinding.ivBarModes2.clearAnimation()
        modesToolbarBinding.ivBarModes3.clearAnimation()
    }

    // Показать боковую панель
    private fun sideBarShow() {
        fragmentManager.beginTransaction().setTransition(TRANSIT_FRAGMENT_OPEN)
            .add(R.id.sideBarContainer, ToolbarSideFragment())
            .addToBackStack("ToolbarSideFragment").commit()
        sideBarFlagHide()
    }

    fun sideBarFlagShow() {
        lifecycleScope.launch {
            delay(100)
            binding.sideBarFlag.isVisible = true
        }
    }

    fun sideBarFlagHide() {
        binding.sideBarFlag.isVisible = false
    }

    fun updateNppList(list: ArrayList<ListRecord>) {
        list.forEachIndexed { index, item -> item.npp = index }
        mainViewModel.updateRecords(list) {}                        // Записать изменения в БД
    }

    private fun initializingRecyclerView() {
        val customizerRecyclerView = CustomizerRecyclerView(this, recycler, adapter)
        customizerRecyclerView.setupRecyclerView()
        mIth = customizerRecyclerView.createItemTouchHelper()
        mIth!!.attachToRecyclerView(recycler)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mIth!!.startDrag(viewHolder)
    }

    // Вызывается из адаптера для возврата Foreground в исходное положение
    override fun returnHolderToOriginalState(viewHolder: RecyclerView.ViewHolder) {
        val foregroundView = (viewHolder as MainViewHolder).llForeground
        foregroundView.translationX = 0f
        foregroundView.translationY = 0f
    }

    fun normBackPressed() {
        adapter.isKeyboardON = false            // Если нажат Back, клавиатура точно скрыта
        noSleepModeOff()                        // Выключение режима БЕЗ СНА
        goToParentDir()                         // Переход к родительской папке
    }

    override fun completionSpecialMode() {
        getMainBuffer().clear()
        getMoveBuffer().clear()
        goToNormalMode()
    }

    // Возврат в режим NORMAL
    fun goToNormalMode() {
        mainViewModel.specialMode = SpecialMode.NORMAL
        enableSpecialMode()
        goToDir(dlAnimator.animOpenNewDir)
    }

    private fun goToParentDir() {
        mainViewModel.getParentDirId(getIdCurrentDir()) { ids ->
            mainViewModel.idDir = ids[0]
            goToDir(dlAnimator.animOpenParentDir)
        }
    }

    override fun goToChildDir(id: Long) {
        mainViewModel.idDir = id
        goToDir(dlAnimator.animOpenChildDir)
    }

    private fun goToDir(animationController: LayoutAnimationController?) {
        binding.progressBar.isVisible = true
        noSleepModeOff()
        mainViewModel.deletingExpiredRecords {
            showList(animationController)
        }
    }

    private fun showList(animationController: LayoutAnimationController?) = lifecycleScope.launch {
        mainViewModel.getRecords { records ->
            fillingRecycler(records, animationController)
            updateFieldsOfSmallToolbar()
            enableSelectAllButtons(getSpecialMode(), records)
            binding.progressBar.isVisible = false
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fillingRecycler(records: List<ListRecord>, animationController: LayoutAnimationController?) {
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

    @SuppressLint("NewApi")
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
        mainBackPressedCallback.isEnabled = false
        floatingBarBackPressedCallback.isEnabled = true
        hideContextMenuDebounce()
    }
    // По скольку метод getLocationOnScreen возвращает значение Y в системе координат с началом отсчета
    // в верхнем левом углу экрана, а контекстное меню вставляется в контейнер с началом отсчета
    // в верхнем левом углу верхней панели инструментов, вводим коррекцию
    private fun getYContextMenu(viewHolder: MainViewHolder): Int {
        val location = IntArray(2)
        topToolbarBinding.llTopToolbar.getLocationOnScreen(location)
        val hStatusBar = location[1]            // Высота строки состояния
        val borderY = heightScreen * 3 / 4      // Граница:
        // для холдеров находящихся НАД ней контекст.меню выводится ПОД холдером,
        // для холдеров находящихся ПОД граеницей - НАД холдером
        viewHolder.llForeground.getLocationOnScreen(location)
        val holderTopY = location[1] - hStatusBar       // Y верхнего угла холдера с коррекцией на высоту строки состояния
        val holderBottomY =
            holderTopY + viewHolder.llForeground.height // Y нижнего угла холдера
        val coordinateY = if (holderTopY < borderY) {   // Если холдер находится над границей
            holderBottomY + 20                          // Контекст.меню выводится на 20 пикселов ниже холдера
        } else {
            holderTopY - 20 - heighContextMenu          // Контекст.меню выводится на 20 пикселов выше холдера
        }
        return coordinateY                              // Y точки, в которую надо вывести контекст.меню
    }

    override fun updateFieldsOfSmallToolbar() {
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
                        delay(5100)
                        topToolbarBinding.pathDir.text = nameDir
                    }
                }

            }
        }
    }

    // Удаление выбранных записей, если у выбранных записей есть вложенные - выдать предупреждение
    @SuppressLint("InflateParams")
    override fun deleteRecords(records: List<ListRecord>) {
        binding.progressBar.isVisible = true
        mainViewModel.selectionSubordinateRecordsToDelete(records) { list ->
            val mutableRecords = list.toMutableList()
            val selectedRecords = records.size
            val subordinateRecords = mutableRecords.size
            mutableRecords.addAll(records)
            if (subordinateRecords != 0) {
                val countArchive = mutableRecords.count { it.isArchive }
                var mess = getString(R.string.del_attempt, selectedRecords.toString(), subordinateRecords.toString())
                val str = if (countArchive != 0) getString(R.string.del_archive, countArchive.toString()) else ""
                mess += "$str."
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_title_attention, null)
                binding.progressBar.isVisible = false
                MaterialAlertDialogBuilder(this)
                    .setCustomTitle(dialogView)
                    .setMessage(mess)
                    .setNeutralButton(getString(R.string.negative_btn)) { d, _ ->
                        d.dismiss() }
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        deleteRecordsAfterSelection(records, mutableRecords)
                    }
                    .show()
            } else {
                deleteRecordsAfterSelection(records, mutableRecords)
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun deleteRecordsAfterSelection(records: List<ListRecord>, mutableRecords: MutableList<ListRecord>) {
        binding.progressBar.isVisible = true
        mutableRecords.forEach { it.isDelete = true }
        mainViewModel.updateRecords(mutableRecords) {
            if (getSpecialMode() == SpecialMode.NORMAL) {
                val position = adapter.records.indexOfFirst { it.id == records[0].id }
                adapter.records.removeAt(position)
                adapter.notifyItemRemoved(position) // Уведомление об удалении
                adapter.notifyItemRangeChanged(position, adapter.records.size - position)
                binding.progressBar.isVisible = false
            } else {
                completionSpecialMode()
                binding.progressBar.isVisible = false
            }
        }
    }

    @SuppressLint("InflateParams")
    fun deleteAllMarks() {
        if (adapter.records.any { it.isChecked }) {
            val dialogView =
                LayoutInflater.from(this).inflate(R.layout.dialog_title_attention, null)
            MaterialAlertDialogBuilder(this)
                .setCustomTitle(dialogView)
                .setMessage(getString(R.string.del_mark_text))
                .setNeutralButton(getString(R.string.back)) { _, _ -> }
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    adapter.records.forEach { it.isChecked = false }
                    val newRecords = adapter.records.sortedWith(compareBy(ListRecord::npp))
                    sortingHoldersAfterChange(newRecords)
                    mainViewModel.updateRecords(adapter.records) {}
                }
                .show()
        } else {
            Toast.makeText(this, getString(R.string.no_marks), Toast.LENGTH_SHORT).show()
        }
    }

    private fun sortingHoldersAfterChange(newRecords: List<ListRecord>) {
        var start: Int
        var finish: Int
        val records = adapter.records
        newRecords.forEachIndexed { index, record ->
            finish = index
            val id = record.id
            start = records.indexOfFirst { it.id == id }
            // Перемещаем запись в массиве адаптера
            records.add(finish, records.removeAt(start))
            // Перемещаем запись на экране
            adapter.notifyItemMoved(start, finish)
            // Сообщение адаптеру, что элемент перемещен
            if (start > finish) {
                adapter.notifyItemRangeChanged(finish, start - finish + 1)
            } else {
                adapter.notifyItemRangeChanged(start, finish - start + 1)
            }
        }
    }

    fun cancelCurrentHolder () {
        adapter.currentHolderIdLiveData.postValue(-1L)
        adapter.currentItem = null
        adapter.currentHolderPosition = -1
    }

    // Показать количество выбранных записей
    override fun showNumberOfSelectedRecords() {
        val number = getMainBuffer().size + getMoveBuffer().size
        modesToolbarBinding.countRecords.text = number.toString()
        val switchOn = number > 0
        modesToolbarBinding.countRecords.isVisible = switchOn
        modesToolbarBinding.btnAction.isEnabled = switchOn
        modesToolbarBinding.btnAction.alpha = if (switchOn) 1.0f else 0.3f
    }

    // Отслеживание состояния прокрутки
    override fun onScrollStateChanged(scrollState: ScrollState) {
        when (scrollState) {
            ScrollState.DOWN -> {           // Прокрутка вниз
                this.scrollState = scrollState
                binding.ivBtnScroll.setImageResource(R.drawable.ic_scroll_down)
                binding.ivBtnScroll.isVisible = true
                binding.tvZoom.visibility = View.GONE
            }
            ScrollState.UP -> {             // Прокрутка вверх
                this.scrollState = scrollState
                binding.ivBtnScroll.setImageResource(R.drawable.ic_scroll_up)
                binding.ivBtnScroll.isVisible = true
                binding.tvZoom.visibility = View.GONE
            }
            ScrollState.STOPPED -> {        // Прокрутка остановлена
                this.scrollState = scrollState
                binding.tvZoom.visibility = View.GONE
            }
            ScrollState.END -> {            // Конец списка
                this.scrollState = scrollState
                binding.ivBtnScroll.isVisible = false
                dlAnimator.animationShowAlpha(binding.tvZoom, 1000)// Анимация появления tvZOOM
            }
        }
        scrollJob.cancel()
        scrollJob = lifecycleScope.launch {
            delay(1000)
            binding.ivBtnScroll.isVisible = false
        }
    }

    // Удаление устаревших напоминаний
    private fun deleteOldAlarm(startTime: Long) {
        val time = startTime + 24 * 60 * 60 * 1000
        lifecycleScope.launch {
            delay(time - System.currentTimeMillis())
            mainViewModel.deleteOldAlarm(time) {
                goToDir(null)
                deleteOldAlarm(time)
            }
        }
    }

    // Перенастройка активити под управление левой рукой
    @SuppressLint("RestrictedApi")
    fun installHandControl() {
        isLeftHandControl = App.appSettings.isLeftHandControl
        val paramsContainer = binding.sideBarContainer.layoutParams as FrameLayout.LayoutParams
        val paramsFlag = binding.sideBarFlag.layoutParams as LinearLayout.LayoutParams
        if (isLeftHandControl) {
            // Центрирование по вертикали и прижатие к началу по горизонтали
            paramsContainer.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            paramsFlag.marginStart = dpToPx(this,0).toInt()
            paramsFlag.marginEnd = dpToPx(this,15).toInt()
            binding.sideBarFlag.scaleX = -1f
        } else {
            // Центрирование по вертикали и прижатие к концу по горизонтали
            paramsContainer.gravity = Gravity.CENTER_VERTICAL or Gravity.END
            paramsFlag.marginStart = dpToPx(this,15).toInt()
            paramsFlag.marginEnd = dpToPx(this,0).toInt()
            binding.sideBarFlag.scaleX = 1f
        }
        binding.sideBarContainer.layoutParams = paramsContainer
        binding.sideBarFlag.layoutParams = paramsFlag
    }

    fun showProgressbar (isShow: Boolean) {
        binding.progressBar.isVisible = isShow
    }

    private fun clickDebounce(): Boolean {
        val current = isClickAllowed
        if (isClickAllowed) {
            isClickAllowed = false
            lifecycleScope.launch {
                delay(5000)
                isClickAllowed = true
            }
        }
        return current
    }

    private fun sideBarDebounce(): Boolean {
        val current = isSideBarOpenAllowed
        if (isSideBarOpenAllowed) {
            isSideBarOpenAllowed = false
            lifecycleScope.launch {
                delay(1000)
                isSideBarOpenAllowed = true
            }
        }
        return current
    }

    fun hideContextMenuDebounce() {
        hideContextMenuJob?.cancel()
        hideContextMenuJob = lifecycleScope.launch {
            delay(HIDE_CONTEXT_MENU_DEBOUNCE_DELAY)
            requestMenuFocus()
        }
    }

    fun getRecords () = adapter.records

    override fun setSpecialMode(mode: SpecialMode) {
        mainViewModel.specialMode = mode
    }

    fun getSpecialMode(): SpecialMode = mainViewModel.specialMode

    override fun getIdCurrentDir(): Long = mainViewModel.idDir

    override fun getMoveBuffer(): ArrayList<ListRecord> = mainViewModel.moveBuffer

    override fun getMainBuffer(): ArrayList<ListRecord> = mainViewModel.mainBuffer

    override fun passRecordToAlarmFragment(record: ListRecord) {
        mainViewModel.record = record
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val id = intent.getLongExtra("IDDIR", -1L)
        if (id != -1L) {
            mainViewModel.idDir = id
            requestMenuFocus()
            goToNormalMode()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        velocityTracker = VelocityTracker.obtain() // Создаем новый объект VelocityTracker
        velocityTracker.addMovement(event)

        if (event != null) {
            specialModeGestureDetector.onTouchEvent(event) // Обработка жестов
        }

        if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_CANCEL) {
            velocityTracker.recycle() // Освобождаем ресурсы
        }
        return super.onTouchEvent(event)
    }
}