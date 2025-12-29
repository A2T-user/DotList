package com.a2t.myapplication.mediafile.data

import com.a2t.myapplication.mediafile.data.dto.MediaItemDto
import com.a2t.myapplication.mediafile.domaim.model.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaFileConverter {
    private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    fun map(item: MediaItemDto): MediaItem  {
        return MediaItem (
            item.uri,
            formatDateFromLongToString(item.creationTime),
            item.mediaFileType
        )
    }

    private fun formatDateFromLongToString (time: Long): String {
        val date = Date(time)
        return dateFormat.format(date)
    }
}