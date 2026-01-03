package com.nigdroid.focusflow_nitr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nigdroid.focusflow_nitr.data.model.AppUsage
import com.nigdroid.focusflow_nitr.databinding.ItemAppUsageBinding
import java.util.concurrent.TimeUnit
import com.nigdroid.focusflow_nitr.R

class AppUsageAdapter : ListAdapter<AppUsage, AppUsageAdapter.ViewHolder>(AppUsageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAppUsageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appUsage: AppUsage) {
            binding.apply {
                appNameText.text = appUsage.appName

                // Format time
                val hours = TimeUnit.MILLISECONDS.toHours(appUsage.timeSpent)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(appUsage.timeSpent) % 60

                appTimeText.text = if (hours > 0) {
                    "${hours}h ${minutes}m"
                } else {
                    "${minutes}m"
                }

                // Set category indicator color
                val categoryColor = when (appUsage.category) {
                    com.nigdroid.focusflow_nitr.data.model.AppCategory.PRODUCTIVE ->
                        R.color.success_green
                    com.nigdroid.focusflow_nitr.data.model.AppCategory.DISTRACTING ->
                        R.color.error_red
                    else -> R.color.text_secondary
                }

                categoryIndicator.setCardBackgroundColor(
                    root.context.getColor(categoryColor)
                )
            }
        }
    }

    class AppUsageDiffCallback : DiffUtil.ItemCallback<AppUsage>() {
        override fun areItemsTheSame(oldItem: AppUsage, newItem: AppUsage): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppUsage, newItem: AppUsage): Boolean {
            return oldItem == newItem
        }
    }
}