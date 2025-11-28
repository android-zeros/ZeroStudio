package com.itsaky.androidide.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils
import com.google.android.material.shape.CornerFamily
import com.itsaky.androidide.databinding.LayoutTemplateListItemBinding
import com.itsaky.androidide.templates.ProjectTemplate
import com.itsaky.androidide.templates.Template

/**
 * RecyclerView.Adapter for showing templates in a grid layout within a category tab.
 *
 * This adapter binds `ProjectTemplate` data to the `layout_template_list_item` view.
 * It displays the template's preview image, title, and description.
 *
 * @param templates The list of templates to display.
 * @param onClick A lambda function to be executed when a template item is clicked.
 *
 * @author android_zero
 *
 * @Work-Flow
 * 1.  `onCreateViewHolder` inflates the item layout.
 * 2.  `onBindViewHolder` populates the views (icon, name, description) with data from the `ProjectTemplate`.
 * 3.  It sets an `onClickListener` to handle user interaction, triggering the provided `onClick` lambda.
 */
class TemplateGridAdapter(
    private val templates: List<ProjectTemplate>,
    private val onClick: (Template<*>) -> Unit
) : RecyclerView.Adapter<TemplateGridAdapter.ViewHolder>() {

    class ViewHolder(internal val binding: LayoutTemplateListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LayoutTemplateListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = templates.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val template = templates[position]
        holder.binding.apply {
            // Set template title
            templateName.text = root.context.getString(template.templateName)

            // Set template preview image
            templateIcon.setImageResource(template.thumb)
            templateIcon.shapeAppearanceModel =
                templateIcon.shapeAppearanceModel.toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, ConvertUtils.dp2px(8f).toFloat())
                    .build()

            // Set template description if available
            val descriptionRes = template.description
            if (descriptionRes != null) {
                templateDescription.text = root.context.getString(descriptionRes)
                templateDescription.visibility = View.VISIBLE
            } else {
                templateDescription.visibility = View.GONE
            }

            root.setOnClickListener {
                onClick.invoke(template)
            }
        }
    }
}