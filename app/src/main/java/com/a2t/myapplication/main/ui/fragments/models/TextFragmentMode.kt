package com.a2t.myapplication.main.ui.fragments.models

import android.os.Parcel
import android.os.Parcelable

enum class TextFragmentMode : Parcelable {
    SEND,
    CONVERT;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TextFragmentMode> {
        override fun createFromParcel(parcel: Parcel): TextFragmentMode {
            return entries[parcel.readInt()]
        }

        override fun newArray(size: Int): Array<TextFragmentMode?> {
            return arrayOfNulls(size)
        }
    }
}