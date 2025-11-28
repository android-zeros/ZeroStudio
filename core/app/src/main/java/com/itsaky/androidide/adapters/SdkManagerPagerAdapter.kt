package com.itsaky.androidide.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.itsaky.androidide.fragments.sdk.SdkListFragment

class SdkManagerPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3 // Platforms, Tools, Update Sites

    override fun createFragment(position: Int): Fragment {
        // For now, all tabs use the same fragment type.
        // We can differentiate them later if needed.
        return SdkListFragment()
    }
}