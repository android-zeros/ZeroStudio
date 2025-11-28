package com.itsaky.androidide.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.itsaky.androidide.app.BaseIDEActivity
import com.itsaky.androidide.databinding.ActivityLottieSelectorBinding
import com.itsaky.androidide.databinding.ItemLottieAnimationBinding
import com.itsaky.androidide.preferences.internal.GeneralPreferences
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.utils.Environment
import com.itsaky.androidide.utils.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * About: Used for loading, importing, and exporting Lottie animation files
 * @author：android_zero
 */
class LottieAnimationSelectorActivity : BaseIDEActivity() {

    private lateinit var binding: ActivityLottieSelectorBinding
    private lateinit var adapter: LottieAnimationAdapter
    private val animationList = mutableListOf<LottieAnimation>()

    private val importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_animation_${System.currentTimeMillis()}.json"
                    val destFile = File(Environment.LOTTIE_ANIMATION_DIR, fileName)

                    inputStream.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input?.copyTo(output)
                        }
                    }
                    // --- FIX START ---
                    Toast.makeText(this, getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                    // --- FIX END ---
                    loadAnimations()
                } catch (e: IOException) {
                    Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding.toolbar.inflateMenu(R.menu.menu_lottie_selector)
        binding.toolbar.setOnMenuItemClickListener { handleMenuItemClick(it) }
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = LottieAnimationAdapter()
        binding.recyclerView.adapter = adapter

        loadAnimations()
    }

    override fun bindLayout(): View {
        binding = ActivityLottieSelectorBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun handleMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_import -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                try {
                    importLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.error_file_picker, e.message), Toast.LENGTH_SHORT).show()
                }
                return true
            }
            R.id.action_export -> {
                showExportDialog()
                return true
            }
        }
        return false
    }

    private fun loadAnimations() {
        animationList.clear()
        
        // Load built-in animations from assets
        try {
            assets.list("LottieAnimation")?.forEach {
                if (it.endsWith(".json")) {
                    animationList.add(LottieAnimation(it, "LottieAnimation/$it", true))
                }
            }
        } catch (e: IOException) {
            // Ignore errors
        }

        // Load imported animations
        if (Environment.LOTTIE_ANIMATION_DIR.exists() && Environment.LOTTIE_ANIMATION_DIR.isDirectory) {
            Environment.LOTTIE_ANIMATION_DIR.listFiles { _, name -> name.endsWith(".json") }?.forEach {
                animationList.add(LottieAnimation(it.name, it.absolutePath, false))
            }
        }
        
        adapter.notifyDataSetChanged()
    }

    private fun showExportDialog() {
        if (animationList.isEmpty()) {
            // --- FIX START ---
            Toast.makeText(this, getString(R.string.no_animations_to_export), Toast.LENGTH_SHORT).show()
            // --- FIX END ---
            return
        }

        val names = animationList.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.action_export)
            .setItems(names) { _, which ->
                exportAnimation(animationList[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun exportAnimation(animation: LottieAnimation) {
        try {
            if (!Environment.LOTTIE_EXPORT_DIR.exists()) {
                Environment.LOTTIE_EXPORT_DIR.mkdirs()
            }
            
            val destPath = File(Environment.LOTTIE_EXPORT_DIR, animation.name).path
            val nonExistPath = FileUtil.getTargetNonExistPath(destPath, false)
            val finalDestFile = File(nonExistPath)

            if (animation.isAsset) {
                // Copy from assets
                assets.open(animation.path).use { input ->
                    FileOutputStream(finalDestFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                // Copy from file system
                val sourceFile = File(animation.path)
                if (!sourceFile.exists()) {
                    throw IOException(getString(R.string.error_file_not_found, animation.name))
                }
                sourceFile.copyTo(finalDestFile, overwrite = true)
            }
            
            Toast.makeText(this, getString(R.string.export_success_to, finalDestFile.absolutePath), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.export_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }

    inner class LottieAnimationAdapter : RecyclerView.Adapter<LottieAnimationAdapter.ViewHolder>() {

        private var selectedPosition: Int = animationList.indexOfFirst { it.path == GeneralPreferences.lottieAnimation }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemLottieAnimationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = animationList[position]
            holder.name.text = item.name
            holder.radioButton.isChecked = position == selectedPosition

            holder.itemView.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = holder.bindingAdapterPosition
                
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    GeneralPreferences.lottieAnimation = animationList[selectedPosition].path
                    Toast.makeText(this@LottieAnimationSelectorActivity, getString(R.string.animation_set_to, item.name), Toast.LENGTH_SHORT).show()
                }
                
                if (oldPosition != -1) {
                    notifyItemChanged(oldPosition)
                }
                if (selectedPosition != -1) {
                    notifyItemChanged(selectedPosition)
                }
                
                finish()
            }
        }

        override fun getItemCount() = animationList.size

        inner class ViewHolder(binding: ItemLottieAnimationBinding) : RecyclerView.ViewHolder(binding.root) {
            val name: TextView = binding.animationName
            val radioButton: RadioButton = binding.radioButton
        }
    }

    data class LottieAnimation(val name: String, val path: String, val isAsset: Boolean)
}