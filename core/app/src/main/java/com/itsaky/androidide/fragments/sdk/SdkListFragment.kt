package com.itsaky.androidide.fragments.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.itsaky.androidide.R
import com.itsaky.androidide.adapters.SdkPackageAdapter
import com.itsaky.androidide.databinding.FragmentSdkListBinding
import com.itsaky.androidide.models.sdk.SdkPackageItem
import com.itsaky.androidide.services.sdk.SdkManagerService
import com.itsaky.androidide.viewmodel.SdkManagerViewModel

/**
 * A fragment that displays a list of SDK packages, allowing users to install or remove them.
 * It observes a shared [SdkManagerViewModel] for data and communicates with [SdkManagerService]
 * to perform background tasks.
 */
class SdkListFragment : Fragment(R.layout.fragment_sdk_list) {

    // Use activityViewModels() to get a ViewModel scoped to the parent Activity.
    private val viewModel: SdkManagerViewModel by activityViewModels()

    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSdkListBinding.bind(view)

        // Initialize the adapter with callbacks for user interactions.
        val sdkAdapter = SdkPackageAdapter(
            onGroupClick = { groupName ->
                // Delegate group expansion/collapse logic to the ViewModel.
                viewModel.toggleGroupExpansion(groupName)
            },
            onActionClick = { item ->
                // Decide whether to download or delete based on the item's installation status.
                if (!item.isInstalled) {
                    viewModel.startDownload(requireContext(), item)
                } else {
                    viewModel.deletePackage(requireContext(), item)
                }
            }
        )

        // Setup the RecyclerView.
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sdkAdapter
            // Use setHasFixedSize(true) for performance optimization if item sizes are constant.
            setHasFixedSize(true) 
        }

        // Observe changes in the package list from the ViewModel and update the adapter.
        viewModel.packageGroups.observe(viewLifecycleOwner) { groups ->
            val items = mutableListOf<Any>()
            groups.forEach { group ->
                items.add(group)
                if (group.isExpanded) {
                    items.addAll(group.packages)
                }
            }
            sdkAdapter.submitList(items)
        }

        // Observe loading state to show/hide the progress bar.
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe toast messages from the ViewModel.
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.onToastShown() // Notify ViewModel that the toast has been shown.
            }
        }
        
        setupBroadcastReceiver()
    }

    /**
     * Initializes the BroadcastReceiver to listen for updates from the SdkManagerService.
     */
    private fun setupBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val url = it.getStringExtra(SdkManagerService.EXTRA_URL)
                    val status = it.getIntExtra(SdkManagerService.EXTRA_STATUS, -1)
                    val progress = it.getIntExtra(SdkManagerService.EXTRA_PROGRESS, 0)
                    val error = it.getStringExtra(SdkManagerService.EXTRA_ERROR)

                    if (url != null && status != -1) {
                        viewModel.handleBroadcastUpdate(url, status, progress, error)
                    }
                }
            }
        }
    }

    /**
     * Registers the BroadcastReceiver when the fragment is resumed.
     */
    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(broadcastReceiver, IntentFilter(SdkManagerService.ACTION_SDK_MANAGER_UPDATE))
    }

    /**
     * Unregisters the BroadcastReceiver when the fragment is paused to prevent memory leaks.
     */
    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver)
    }
}