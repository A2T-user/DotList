package com.a2t.myapplication.mediafile.ui.util

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import com.a2t.myapplication.mediafile.data.dto.MediaFileType
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/* ВНИМАНИЕ!
    Все окна просмотра в макетах и плейсхолдер fragment_select_media_file и fragment_media_viewer должны быть GONE
    Все окна просмотра должны быть перечислены в listPreviewWindows в том же порядке как в when метода loadMedia */

class DownloaderMediaFile(
    private val context: Context,
    private val listPreviewWindows: List<View>,
    private val placeholder: ImageView
) {

    fun loadMedia(mediaFileType: MediaFileType, uri: Uri?, file: File?) {
        var window: View?
        when (mediaFileType) {
            MediaFileType.IMAGE -> {
                window = listPreviewWindows[0] as SubsamplingScaleImageView
                try {
                    val bitmap = if (uri != null) {
                        getBitmap(context, uri)
                    } else {
                        getBitmap(file!!)
                    }
                    if (bitmap == null || bitmap.isRecycled) {
                        errorLoadingFile(true, window, placeholder)
                        return
                    }
                    errorLoadingFile(false, window, placeholder)
                    val rotatedBitmap = if (uri != null) {
                        rotateBitmapAccordingToExif(context, uri, bitmap)
                    } else {
                        rotateBitmapAccordingToExif(file!!, bitmap)
                    }
                    window.setImage(ImageSource.bitmap(rotatedBitmap))
                    window.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                    window.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF)
                    window.setDoubleTapZoomScale(2f)
                    window.setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
                } catch (_: Exception) {
                    errorLoadingFile(true, window, placeholder)
                }
            }
            else -> {
                placeholder.isVisible = true
            }
        }
    }

    private fun errorLoadingFile(isError: Boolean, window: View, placeholder: ImageView) {
        window.isVisible = !isError
        placeholder.isVisible = isError
    }

    private fun getBitmap(context: Context, uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }
    private fun getBitmap(file: File): Bitmap {
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun rotateBitmapAccordingToExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }

                if (rotationDegrees != 0) {
                    val matrix = Matrix()
                    matrix.postRotate(rotationDegrees.toFloat())
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }
            } ?: bitmap // если inputStream == null — возвращаем оригинал
        } catch (_: Exception) {
            bitmap
        }
    }
    private fun rotateBitmapAccordingToExif(file: File, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotationDegrees != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (_: Exception) {
            bitmap
        }
    }

}
