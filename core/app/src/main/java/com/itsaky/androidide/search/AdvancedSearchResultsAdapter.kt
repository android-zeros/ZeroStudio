package com.itsaky.androidide.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.itsaky.androidide.R
import com.itsaky.androidide.databinding.ItemSearchFileHeaderBinding
import com.itsaky.androidide.databinding.ItemSearchTextMatchBinding
import com.itsaky.androidide.models.FileExtension
import com.itsaky.androidide.search.*
import com.itsaky.androidide.utils.resolveAttr

/**
 * 搜索结果列表适配器
 */
class AdvancedSearchResultsAdapter(
    private val onItemClick: (SearchResultItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = ArrayList<SearchResultItem>()

    companion object {
        private const val TYPE_FILE_HEADER = 0
        private const val TYPE_TEXT_MATCH = 1
    }

    fun submitList(newItems: List<SearchResultItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is FileHeaderResult -> TYPE_FILE_HEADER
            is TextMatchResult -> TYPE_TEXT_MATCH
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FILE_HEADER -> FileHeaderHolder(ItemSearchFileHeaderBinding.inflate(inflater, parent, false))
            TYPE_TEXT_MATCH -> TextMatchHolder(ItemSearchTextMatchBinding.inflate(inflater, parent, false))
            else -> throw IllegalStateException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        holder.itemView.setOnClickListener { onItemClick(item) }

        if (holder is FileHeaderHolder && item is FileHeaderResult) {
            holder.bind(item)
        } else if (holder is TextMatchHolder && item is TextMatchResult) {
            holder.bind(item)
        }
    }

    override fun getItemCount() = items.size

    class FileHeaderHolder(val binding: ItemSearchFileHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FileHeaderResult) {
            // 设置图标
            val extension = FileExtension.Factory.forFile(item.file)
            binding.fileIcon.setImageResource(extension.icon)
            
            // 设置路径 (相对路径显示更友好)
            binding.filePath.text = item.file.name
            binding.fileDir.text = item.file.parent
            
            binding.matchCount.text = if (item.matchCount > 0) "${item.matchCount}" else ""
            binding.moduleName.text = item.moduleName ?: ""
            binding.moduleName.visibility = if (item.moduleName != null) View.VISIBLE else View.GONE
        }
    }

    class TextMatchHolder(val binding: ItemSearchTextMatchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TextMatchResult) {
            // 设置行号
            binding.lineNumber.text = (item.lineIndex + 1).toString()
            
            // 设置高亮预览文本
            binding.codePreview.text = item.previewText
            
            // 拥挤模式处理
            if (item.isCrowded) {
                // 拥挤模式：仅显示一行，去除多余 padding
                binding.root.setPadding(0, 0, 0, 0)
                binding.codePreview.maxLines = 1
            } else {
                // 正常模式：显示更多细节
                binding.root.setPadding(0, 8, 0, 8)
                binding.codePreview.maxLines = 3
            }
        }
    }
}