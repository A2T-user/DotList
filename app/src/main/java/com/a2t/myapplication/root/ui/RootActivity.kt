package com.a2t.myapplication.root.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.a2t.myapplication.App
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.ActivityRootBinding
import com.a2t.myapplication.root.presentation.model.SpecialMode
import com.a2t.myapplication.main.ui.MainFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RootActivity : AppCompatActivity() {
    var idDir: Long? = null
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var binding: ActivityRootBinding
    private lateinit var navController: NavController
    private lateinit var mainBackPressedCallback: OnBackPressedCallback

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        idDir = intent.getLongExtra("IDDIR",0L)
        Log.e("МОЁ", "onCreate idDir = "+idDir)
        binding = ActivityRootBinding.inflate(layoutInflater)

        setContentView(binding.root)

        navHostFragment = supportFragmentManager.findFragmentById(R.id.container_view) as NavHostFragment
        navController = navHostFragment.navController

        // $$$$$$$$$$$$$$$$$$$$$$   Реакция на нажатие системной кнопки BACK   $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        mainBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                if (currentFragment is MainFragment) {
                    when(currentFragment.getSpecialMode()) {
                        SpecialMode.DELETE, SpecialMode.RESTORE -> {
                            currentFragment.completionSpecialMode()
                        }
                        else -> {
                            if (currentFragment.getIdCurrentDir() > 0) {
                                currentFragment.mainBackPressed()
                            } else {                                    // Выход по двойному нажатию Back
                                Toast.makeText(this@RootActivity, R.string.text_exit, Toast.LENGTH_SHORT).show() // Сообщение
                                mainBackPressedCallback.isEnabled = false
                                // Сбросить первое касание через 2 секунды
                                lifecycleScope.launch {
                                    delay(2000)
                                    mainBackPressedCallback.isEnabled = true
                                }
                            }
                        }
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, mainBackPressedCallback)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.mainFragment -> mainBackPressedCallback.isEnabled = true
                else -> mainBackPressedCallback.isEnabled = false
            }
        }
    }

    // Включение/выключение режима БЕЗ СНА
    fun switchNoSleepMode(isOn: Boolean) {
        binding.rootLayout.keepScreenOn = isOn
    }

    // Вывод пояснительных сообщений
    fun showHintToast(text: String, duration: Int) {
        if (App.appSettings.hintToastOn) Toast.makeText(this, text, duration).show()
    }

    override fun onStart() {
        super.onStart()
        (applicationContext as App).getSettings()
    }
}