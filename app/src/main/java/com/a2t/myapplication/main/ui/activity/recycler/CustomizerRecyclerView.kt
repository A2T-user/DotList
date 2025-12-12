package com.a2t.myapplication.main.ui.activity.recycler

import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.R
import com.a2t.myapplication.main.domain.model.ListRecord
import com.a2t.myapplication.main.ui.ActionEditText
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.main.ui.activity.model.SpecialMode
import java.util.Collections
import kotlin.math.abs

class CustomizerRecyclerView(
    val ma: MainActivity,
    val recycler: RecyclerView,
    val adapter: MainAdapter
) {
    companion object {
        const val K_MAX_SHIFT_RIGHT = 0.2f
        const val K_MAX_SHIFT_LEFT = -0.3f
    }

    fun setupRecyclerView() {
        val animOpenNewDir = AnimationUtils.loadLayoutAnimation(ma, R.anim.anim_open_new_dir)
        recycler.adapter = adapter
        recycler.setLayoutManager(object : LinearLayoutManager(ma) {
            // Разрешаем скольжение тоько при старте редактирования записи
            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean,
                focusedChildVisible: Boolean
            ): Boolean {
                if (ma.currentFocus is ActionEditText)
                    super.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible)
                return false
            }
        })
        recycler.itemAnimator = DefaultItemAnimator().apply {
            moveDuration = 300
            removeDuration = 100
        }
        recycler.scheduleLayoutAnimation()
        recycler.layoutAnimation = animOpenNewDir
        recycler.invalidate()
    }

    fun createItemTouchHelper(): ItemTouchHelper {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            val widthScreen = ma.widthScreen
            val maxShiftToRight = widthScreen * K_MAX_SHIFT_RIGHT               // Величина максимального смещения при свайпе в право
            val maxShiftToLeft = widthScreen * K_MAX_SHIFT_LEFT                 // Величина максимального смещения при свайпе в лево
            var isSwipe = false
            var isMove = false
            override fun isLongPressDragEnabled(): Boolean {        // Запретить Drag по LongClick (у нас перетаскивание за контроллер)
                return false
            }
            // Разрешить Swipe
            override fun isItemViewSwipeEnabled(): Boolean {
                return ma.getSpecialMode() == SpecialMode.NORMAL            // Swipe будет только в нормальном режиме
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
                    if (!item.isNew  && ma.getSpecialMode() == SpecialMode.NORMAL) {
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
                    ma.updateNppList(listDir) // Обновить порядковые номера записей в массиве и БД
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
                    val backgroundView = (viewHolder as MainViewHolder).llBackground
                    if (!backgroundView.hasFocus()) backgroundView.requestFocus()
                } else {
                    ma.returnHolderToOriginalState(viewHolder)
                }
                // Сортировка по меткам
                if (isMove) ma.correctingPositionOfRecordByCheck(viewHolder as MainViewHolder)
            }
        }
        return ItemTouchHelper(callback)
    }

    private fun getItemById(id: Long, records: List<ListRecord>): ListRecord? {
        return records.find { it.id == id }
    }

}