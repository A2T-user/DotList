package com.a2t.myapplication.common.model

import android.view.View
import android.view.animation.AnimationUtils
import com.a2t.myapplication.R
import com.a2t.myapplication.common.App

class DLAnimator {
    val animationMoveMode = AnimationUtils.loadAnimation(App.appContext, R.anim.arrow_right)!!
    val animationDeleteMode = AnimationUtils.loadAnimation(App.appContext, R.anim.arrow_down)!!
    val animationRestoreMode = AnimationUtils.loadAnimation(App.appContext, R.anim.arrow_up)!!
    val animationArchiveMode = AnimationUtils.loadAnimation(App.appContext, R.anim.archive)!!
    val animationEye = AnimationUtils.loadAnimation(App.appContext, R.anim.eye_eff)!!
    val animOpenNewDir = AnimationUtils.loadLayoutAnimation(App.appContext, R.anim.anim_open_new_dir)!!
    val animOpenChildDir = AnimationUtils.loadLayoutAnimation(App.appContext, R.anim.anim_open_child_dir)!!
    val animOpenParentDir = AnimationUtils.loadLayoutAnimation(App.appContext, R.anim.anim_open_parent_dir)!!
    val animBell = AnimationUtils.loadAnimation(App.appContext, R.anim.anim_bell)!!
    val animRecord = AnimationUtils.loadAnimation(App.appContext, R.anim.holder_move)!!
    val animDelHolder = AnimationUtils.loadAnimation(App.appContext, R.anim.del_holder)!!

    fun animationShowAlpha(view: View, duration: Long) {
        view.visibility = View.VISIBLE // Делаем видимым перед анимацией
        view.alpha = 0f     // Начальное значение alpha
        view.scaleX = 0f    // Начальный масштаб по оси X
        view.scaleY = 0f    // Начальный масштаб по оси Y
        view.animate()
            .alpha(1f) // Конечное значение alpha
            .scaleX(1f) // Конечное значение масштаба по оси X
            .scaleY(1f) // Конечное значение масштаба по оси Y
            .setDuration(duration) // Длительность анимации в миллисекундах
            .start()
    }

    fun animationHideAlpha(view: View, duration: Long) {
        view.animate()
            .alpha(0f) // Конечное значение alpha
            .setDuration(duration) // Длительность анимации в миллисекундах
            .withEndAction {
                view.visibility = View.GONE // Делаем видимым перед анимацией
                view.alpha = 1f
            }
            .start()
    }

}