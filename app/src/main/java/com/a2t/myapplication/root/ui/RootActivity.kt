package com.a2t.myapplication.root.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.a2t.myapplication.App
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.ActivityRootBinding
import com.a2t.myapplication.main.ui.MainFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RootActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRootBinding
    private lateinit var navController: NavController
    private lateinit var mainBackPressedCallback: OnBackPressedCallback
    private var backJob: Job? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // $$$$$$$$$$$$$$$$$$$$$$   Реакция на нажатие системной кнопки BACK   $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        val mainBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragment: MainFragment? = supportFragmentManager.findFragmentById(R.id.mainFragment) as MainFragment?
                if (fragment != null) {
                    if (fragment.idDir > 0) {
                        fragment.mainBackPressed()
                    } else {                                    // Выход по двойному нажатию Back
                        showHintToast(getString(R.string.text_exit), 5000) // Сообщение
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
        onBackPressedDispatcher.addCallback(this, mainBackPressedCallback)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.container_view) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mainFragment -> mainBackPressedCallback.isEnabled = true
                else -> mainBackPressedCallback.isEnabled = false
            }
        }
    }

    // Включение/выключение режима БЕЗ СНА
    fun switchNoSleepMode (isOn: Boolean) {
        binding.rootLayout.keepScreenOn = isOn
    }

    // Вывод пояснительных сообщений
    fun showHintToast (text: String, duration: Int) {
        if (App.appSettings.hintToastOn) Toast.makeText(this, text, duration).show()
    }

    override fun onStart() {
        super.onStart()
        (applicationContext as App).getSettings()
    }
}