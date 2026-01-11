package com.a2t.myapplication.mediafile.data.dto

import android.net.Uri

sealed interface Response {
    data class Success(val fileName: String): Response
    data class FileExists(val originalName: String, val uri: Uri, val mediaFileType: MediaFileType): Response
    data class Error(val errCode: ErrCode): Response
}
