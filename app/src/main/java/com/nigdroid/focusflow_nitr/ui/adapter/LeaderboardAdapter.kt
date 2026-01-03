package com.nigdroid.focusflow_nitr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.databinding.ItemLeaderboardBinding
import com.nigdroid.focusflow_nitr.ui.leaderboard.LeaderboardEntry

class LeaderboardAdapter : ListAdapter<LeaderboardEntry, LeaderboardAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemLeaderboardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LeaderboardEntry) {
            binding.apply {
                rankText.text = "#${entry.rank}"
                nameText.text = entry.name
                hoursText.text = "${String.format("%.1f", entry.hours)}h"
                pointsText.text = "${entry.points} pts"

                // Load avatar
                if (entry.avatarUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(entry.avatarUrl)
                        .circleCrop()
                        .into(avatarImage)
                } else {
                    avatarImage.setImageResource(R.drawable.ic_profile)
                }

                // Show rank change
                when {
                    entry.change > 0 -> {
                        changeIcon.text = "⬆️"
                        changeText.text = "+${entry.change}"
                        changeText.setTextColor(root.context.getColor(R.color.success_green))
                    }
                    entry.change < 0 -> {
                        changeIcon.text = "⬇️"
                        changeText.text = "${entry.change}"
                        changeText.setTextColor(root.context.getColor(R.color.error_red))
                    }
                    else -> {
                        changeIcon.text = "➡️"
                        changeText.text = ""
                    }
                }

                // Highlight top 3
                when (entry.rank) {
                    1 -> rankBadge.text = "🥇"
                    2 -> rankBadge.text = "🥈"
                    3 -> rankBadge.text = "🥉"
                    else -> rankBadge.text = ""
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LeaderboardEntry>() {
        override fun areItemsTheSame(oldItem: LeaderboardEntry, newItem: LeaderboardEntry): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: LeaderboardEntry, newItem: LeaderboardEntry): Boolean {
            return oldItem == newItem
        }
    }
}