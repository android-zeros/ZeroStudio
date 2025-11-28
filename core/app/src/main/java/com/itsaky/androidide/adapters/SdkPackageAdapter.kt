package com.itsaky.androidide.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.itsaky.androidide.R
import com.itsaky.androidide.databinding.ItemSdkGroupBinding
import com.itsaky.androidide.databinding.ItemSdkPackageBinding
import com.itsaky.androidide.models.sdk.SdkPackageGroup
import com.itsaky.androidide.models.sdk.SdkPackageItem

/**
 * An adapter for the SDK Manager RecyclerView, which handles displaying
 * expandable groups (SdkPackageGroup) and individual package items (SdkPackageItem).
 *
 * @param onGroupClick A lambda function to be invoked when a group header is clicked.
 * @param onActionClick A lambda function to be invoked when the action button (download/delete) on an item is clicked.
 */
class SdkPackageAdapter(
    private val onGroupClick: (String) -> Unit,
    private val onActionClick: (SdkPackageItem) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(SdkDiffCallback()) {

    private val VIEW_TYPE_GROUP = 0
    private val VIEW_TYPE_ITEM = 1

    /**
     * Determines the view type for the item at the given position.
     */
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SdkPackageGroup -> VIEW_TYPE_GROUP
            is SdkPackageItem -> VIEW_TYPE_ITEM
            else -> throw IllegalArgumentException("Unknown type at position $position")
        }
    }

    /**
     * Creates a new ViewHolder for a given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_GROUP -> {
                val binding = ItemSdkGroupBinding.inflate(inflater, parent, false)
                GroupViewHolder(binding, onGroupClick)
            }
            VIEW_TYPE_ITEM -> {
                val binding = ItemSdkPackageBinding.inflate(inflater, parent, false)
                ItemViewHolder(binding, onActionClick)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    /**
     * Binds data to the ViewHolder at the specified position.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GroupViewHolder -> holder.bind(getItem(position) as SdkPackageGroup)
            is ItemViewHolder -> holder.bind(getItem(position) as SdkPackageItem)
        }
    }

    /**
     * ViewHolder for displaying an SdkPackageGroup. Handles click events to expand/collapse the group.
     */
    class GroupViewHolder(
        private val binding: ItemSdkGroupBinding,
        private val onGroupClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: SdkPackageGroup) {
            binding.groupName.text = group.name
            binding.groupDescription.text = group.description
            binding.expandIcon.setImageResource(
                if (group.isExpanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_right
            )
            binding.root.setOnClickListener {
                onGroupClick(group.name)
            }
        }
    }

    /**
     * ViewHolder for displaying an SdkPackageItem. Handles UI state changes based on the item's
     * installation and download status, and forwards action clicks.
     */
    class ItemViewHolder(
        private val binding: ItemSdkPackageBinding,
        private val onActionClick: (SdkPackageItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SdkPackageItem) {
            binding.packageName.text = item.name
            
            when {
                item.isDownloading -> {
                    binding.packageStatus.text = itemView.context.getString(R.string.status_downloading)
                    binding.actionButton.visibility = View.GONE
                    binding.progressBar.visibility = View.VISIBLE
                    // Use indeterminate progress bar for states like "extracting"
                    binding.progressBar.isIndeterminate = item.downloadProgress < 0
                    binding.progressBar.progress = if (item.downloadProgress >= 0) item.downloadProgress else 0
                }
                item.isInstalled -> {
                    binding.packageStatus.text = itemView.context.getString(R.string.status_installed)
                    binding.actionButton.setImageResource(R.drawable.ic_delete)
                    binding.actionButton.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
                else -> { // Not installed and not downloading
                    binding.packageStatus.text = itemView.context.getString(R.string.status_not_installed)
                    binding.actionButton.setImageResource(R.drawable.ic_download)
                    binding.actionButton.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
            }

            binding.actionButton.setOnClickListener {
                // Prevent clicking while a download is in progress
                if (!item.isDownloading) {
                    onActionClick(item)
                }
            }
        }
    }
}

/**
 * DiffUtil.ItemCallback implementation for efficiently updating the list.
 * It compares items to determine if the list content has changed.
 */
class SdkDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is SdkPackageGroup && newItem is SdkPackageGroup -> oldItem.name == newItem.name
            oldItem is SdkPackageItem && newItem is SdkPackageItem -> oldItem.downloadUrl == newItem.downloadUrl
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        // Since our data classes are used for state management (copy), a simple equality check is sufficient and efficient.
        return oldItem == newItem
    }
}