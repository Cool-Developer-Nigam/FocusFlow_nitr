package com.nigdroid.focusflow_nitr.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nigdroid.focusflow_nitr.data.model.Goal
import com.nigdroid.focusflow_nitr.databinding.ItemGoalBinding
import java.util.concurrent.TimeUnit

class GoalsAdapter(
    private val onGoalClick: (Goal) -> Unit,
    private val onMenuClick: (Goal, View) -> Unit
) : ListAdapter<Goal, GoalsAdapter.GoalViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GoalViewHolder(
        private val binding: ItemGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: Goal) {
            binding.apply {
                goalTitle.text = goal.title
                goalSubtitle.text = "Target: ${goal.targetHours} hours"

                val progressHours = TimeUnit.MILLISECONDS.toHours(goal.currentProgress).toInt()
                val percentage = ((progressHours.toFloat() / goal.targetHours) * 100).toInt()
                    .coerceAtMost(100)

                goalProgress.progress = percentage
                progressText.text = "$progressHours / ${goal.targetHours} hours"
                progressPercentage.text = "$percentage%"

                val daysLeft = TimeUnit.MILLISECONDS.toDays(
                    goal.deadline - System.currentTimeMillis()
                )
                deadlineText.text = when {
                    daysLeft < 0 -> "Overdue"
                    daysLeft == 0L -> "Due today"
                    daysLeft == 1L -> "Due tomorrow"
                    else -> "Ends in $daysLeft days"
                }

                root.setOnClickListener { onGoalClick(goal) }
                goalMenu.setOnClickListener { onMenuClick(goal, it) }
            }
        }
    }

    class GoalDiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(oldItem: Goal, newItem: Goal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Goal, newItem: Goal): Boolean {
            return oldItem == newItem
        }
    }
}