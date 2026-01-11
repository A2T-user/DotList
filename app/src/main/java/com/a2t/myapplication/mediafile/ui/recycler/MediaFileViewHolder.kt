package com.a2t.myapplication.mediafile.ui.recycler

import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.a2t.myapplication.R
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.a2t.myapplication.mediafile.domaim.model.MediaItem
import com.bumptech.glide.Glide
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MediaFileViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var observerItem: Observer<MediaItem?>? = null
    // Получаем текущую дату
    val currentDate: LocalDate = LocalDate.now()
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy", Locale.getDefault())
    val today: String = currentDate.format(formatter)

    val container: ConstraintLayout = itemView.findViewById(R.id.cl_holder)
    val image: ImageView = itemView.findViewById(R.id.iv_image)
    val date: TextView = itemView.findViewById(R.id.tv_date)
    val check: ImageView = itemView.findViewById(R.id.iv_check)

    fun bind(item: MediaItem) {
        // Вставка рисунка
        Glide.with(itemView)
            .load(item.uri)
            .error(R.drawable.ic_error)       // Опционально: изображение при ошибке
            .centerCrop()  // Опционально: масштабирование (centerCrop, fitCenter и т.д.)
            .into(image)
        // Установка значка для видио и pdf
        when (item.mediaFileType) {
            MediaFileType.VIDEO -> {
                image.foreground = AppCompatResources.getDrawable(itemView.context, R.drawable.ic_play)
            }
            MediaFileType.IMAGE -> {
                image.foreground = null
            }
        }
        // Выделить синим шрифтом файлы с сегодняшней датой
        if (item.creationDate == today) {
            date.text = itemView.context.getString(R.string.today)
        } else {
            date.text = item.creationDate
        }
    }

    // Меняем отступы фото, цвет фона и выводим галочку в выбранном holder
    fun selectHolder(isSelected: Boolean) {
        val backgroundColor: Int
        var marginInDp: Int
        if (isSelected) {
            backgroundColor = ResourcesCompat.getColor(itemView.resources, R.color.blue_icon, null)
            marginInDp = SELECTED_HOLDER_MARGIN
        } else {
            backgroundColor = ResourcesCompat.getColor(itemView.resources, R.color.transparent, null)
            marginInDp = NORMAL_HOLDER_MARGIN
        }
        check.isVisible = isSelected
        container.setBackgroundColor(backgroundColor)
        val marginInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginInDp.toFloat(),
            itemView.resources.displayMetrics
        ).toInt()
        val params = image.layoutParams as? ConstraintLayout.LayoutParams
        params?.let {
            it.setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
            image.layoutParams = it
        }
    }

    companion object {
        const val NORMAL_HOLDER_MARGIN = 0
        const val SELECTED_HOLDER_MARGIN = 4
    }
}