package com.a2t.myapplication.mediafile.data.dto

sealed interface Response {
    data class Success(val fileName: String): Response
    data class FileExists(val fileName: String): Response
    data class Error(val errCode: ErrCode): Response
}
