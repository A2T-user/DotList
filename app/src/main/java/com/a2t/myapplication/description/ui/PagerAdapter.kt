package com.a2t.myapplication.description.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.a2t.myapplication.description.ui.fragments.AlarmFragment7
import com.a2t.myapplication.description.ui.fragments.ArchiveFragment13
import com.a2t.myapplication.description.ui.fragments.ConvertFragment14
import com.a2t.myapplication.description.ui.fragments.DelModeFragment11
import com.a2t.myapplication.description.ui.fragments.DirsFragment4
import com.a2t.myapplication.description.ui.fragments.GeneralInformationFragment1
import com.a2t.myapplication.description.ui.fragments.LinesFragment3
import com.a2t.myapplication.description.ui.fragments.MainToolbarFragment8
import com.a2t.myapplication.description.ui.fragments.MoveModeFragment10
import com.a2t.myapplication.description.ui.fragments.NavFragment5
import com.a2t.myapplication.description.ui.fragments.RecordsFragment2
import com.a2t.myapplication.description.ui.fragments.RestoreModeFragment12
import com.a2t.myapplication.description.ui.fragments.SideToolbarFragment9
import com.a2t.myapplication.description.ui.fragments.TextSizeFragment6

class PagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 14

    override fun createFragment(position: Int): Fragment {
        return  when (position + 1) {
            1 -> GeneralInformationFragment1()
            2 -> RecordsFragment2()
            3 -> LinesFragment3()
            4 -> DirsFragment4()
            5 -> NavFragment5()
            6 -> TextSizeFragment6()
            7 -> AlarmFragment7()
            8 -> MainToolbarFragment8()
            9 -> SideToolbarFragment9()
            10 -> MoveModeFragment10()
            11 -> DelModeFragment11()
            12 -> RestoreModeFragment12()
            13 -> ArchiveFragment13()
            else -> ConvertFragment14()
        }
    }
}