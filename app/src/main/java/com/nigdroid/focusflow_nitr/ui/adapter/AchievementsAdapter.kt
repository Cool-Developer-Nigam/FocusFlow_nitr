package com.nigdroid.focusflow_nitr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nigdroid.focusflow_nitr.data.model.Achievement
import com.nigdroid.focusflow_nitr.databinding.ItemAchievementBinding

class AchievementsAdapter : ListAdapter<Achievement, AchievementsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchievementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemAchievementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(achievement: Achievement) {
            binding.apply {
                achievementIcon.text = achievement.iconName
                achievementName.text = achievement.name
                achievementDescription.text = achievement.description
                pointsText.text = "+${achievement.points} pts"

                if (achievement.isUnlocked) {
                    // Unlocked state
                    root.alpha = 1.0f
                    lockIcon.visibility = android.view.View.GONE
                    progressBar.visibility = android.view.View.GONE
                    progressText.visibility = android.view.View.GONE
                    unlockedBadge.visibility = android.view.View.VISIBLE
                } else {
                    // Locked state
                    root.alpha = 0.6f
                    lockIcon.visibility = android.view.View.VISIBLE
                    progressBar.visibility = android.view.View.VISIBLE
                    progressText.visibility = android.view.View.VISIBLE
                    unlockedBadge.visibility = android.view.View.GONE

                    // Show progress
                    val progressPercent = if (achievement.requirement > 0) {
                        (achievement.progress.toFloat() / achievement.requirement * 100).toInt()
                    } else 0

                    progressBar.max = 100
                    progressBar.progress = progressPercent
                    progressText.text = "${achievement.progress}/${achievement.requirement}"
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Achievement>() {
        override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem.achievementId == newItem.achievementId
        }

        override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
            return oldItem == newItem
        }
    }
}
