package com.itsaky.androidide.activities

import android.os.Bundle
import android.view.View
import androidx.core.graphics.Insets
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.itsaky.androidide.R
import com.itsaky.androidide.adapters.SdkManagerPagerAdapter
import com.itsaky.androidide.app.EdgeToEdgeIDEActivity
import com.itsaky.androidide.databinding.ActivitySdkManagerBinding

/**
 * SdkManagerActivity is the main screen for managing Android SDK components.
 * It uses a ViewPager2 with a TabLayout to display different categories of SDK packages.
 *
 * @author android_zero
 */
class SdkManagerActivity : EdgeToEdgeIDEActivity() {

    private var _binding: ActivitySdkManagerBinding? = null
    private val binding: ActivitySdkManagerBinding
        get() = checkNotNull(_binding) {
            "ActivitySdkManagerBinding was not inflated or has been destroyed."
        }

    override fun bindLayout(): View {
        _binding = ActivitySdkManagerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        setupViewPagerAndTabs()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.sdk_manager)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupViewPagerAndTabs() {
        // Initialize the PagerAdapter
        val pagerAdapter = SdkManagerPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Link the TabLayout with the ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.sdk_platforms)
                1 -> getString(R.string.sdk_tools)
                2 -> getString(R.string.sdk_update_sites)
                else -> null
            }
        }.attach()
    }

    /**
     * Applies system bar insets to the layout to ensure proper padding in edge-to-edge mode.
     */
    override fun onApplySystemBarInsets(insets: Insets) {
        // Apply top padding to the AppBarLayout to account for the status bar
        binding.appBarLayout.setPadding(insets.left, insets.top, insets.right, 0)

        // Apply bottom padding to the ViewPager to account for the navigation bar
        binding.viewPager.setPadding(insets.left, 0, insets.right, insets.bottom)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}