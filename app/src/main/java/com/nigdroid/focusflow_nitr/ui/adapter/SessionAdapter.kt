package com.nigdroid.focusflow_nitr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nigdroid.focusflow_nitr.data.model.Session
import com.nigdroid.focusflow_nitr.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SessionAdapter : ListAdapter<Session, SessionAdapter.ViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionBinding.inflate(
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
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: Session) {
            binding.apply {
                // Set icon based on session type
                sessionIcon.text = when (session.sessionType) {
                    com.nigdroid.focusflow_nitr.data.model.SessionType.STUDY -> "📚"
                    com.nigdroid.focusflow_nitr.data.model.SessionType.POMODORO -> "🍅"
                    com.nigdroid.focusflow_nitr.data.model.SessionType.BREAK -> "☕"
                }

                // Set title
                sessionTitle.text = when (session.sessionType) {
                    com.nigdroid.focusflow_nitr.data.model.SessionType.STUDY -> "Focus Session"
                    com.nigdroid.focusflow_nitr.data.model.SessionType.POMODORO -> "Pomodoro Session"
                    com.nigdroid.focusflow_nitr.data.model.SessionType.BREAK -> "Break"
                }

                // Format duration
                val hours = TimeUnit.MILLISECONDS.toHours(session.duration)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(session.duration) % 60

                sessionDuration.text = when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    minutes > 0 -> "${minutes}m"
                    else -> "< 1m"
                }

                // Format time ago
                sessionTime.text = getTimeAgo(session.startTime.toDate().time)
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours ${if (hours == 1L) "hour" else "hours"} ago"
                }
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.sessionId == newItem.sessionId
        }

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem == newItem
        }
    }
}