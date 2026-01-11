package com.a2t.myapplication.mediafile.presentation.model

import com.a2t.myapplication.mediafile.data.dto.DirType
import com.a2t.myapplication.mediafile.data.dto.MediaFileType

data class MediaFileFilter (
    var dir: DirType,
    var type: MediaFileType?
)
