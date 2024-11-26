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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RootActivity : AppCompatActivity() {
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var binding: ActivityRootBinding
    private lateinit var navController: NavController
    private lateinit var rootBackPressedCallback: OnBackPressedCallback

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navHostFragment = supportFragmentManager.findFragmentById(R.id.container_view) as NavHostFragment
        navController = navHostFragment.navController

        // $$$$$$$$$$$$$$$$$$$$$$   Реакция на нажатие системной кнопки BACK   $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        rootBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                if (fragment is MainFragment) {
                    if (fragment.idDir > 0) {
                        fragment.mainBackPressed()
                    } else {                                    // Выход по двойному нажатию Back
                        Toast.makeText(this@RootActivity, R.string.text_exit, Toast.LENGTH_SHORT).show() // Сообщение
                        rootBackPressedCallback.isEnabled = false
                        // Сбросить первое касание через 2 секунды
                        lifecycleScope.launch {
                            delay(2000)
                            rootBackPressedCallback.isEnabled = true
                        }
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, rootBackPressedCallback)
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