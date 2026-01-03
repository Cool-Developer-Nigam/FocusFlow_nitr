package com.nigdroid.focusflow_nitr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nigdroid.focusflow_nitr.data.model.Reflection
import com.nigdroid.focusflow_nitr.databinding.ItemReflectionBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReflectionsAdapter(
    private val onDeleteClick: (Reflection) -> Unit
) : ListAdapter<Reflection, ReflectionsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReflectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemReflectionBinding,
        private val onDeleteClick: (Reflection) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reflection: Reflection) {
            binding.apply {
                // Format date
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dateText.text = sdf.format(reflection.date.toDate())

                // Set mood emoji
                moodEmoji.text = getMoodEmoji(reflection.mood)

                // Set note
                noteText.text = reflection.textNote

                // Delete button
                deleteButton.setOnClickListener {
                    onDeleteClick(reflection)
                }

                // Voice note indicator
                if (reflection.voiceNoteUrl.isNotEmpty()) {
                    voiceNoteIndicator.visibility = android.view.View.VISIBLE
                } else {
                    voiceNoteIndicator.visibility = android.view.View.GONE
                }
            }
        }

        private fun getMoodEmoji(mood: String): String {
            return when (mood.lowercase()) {
                "great", "happy", "excellent" -> "😊"
                "good" -> "🙂"
                "okay", "fine" -> "😐"
                "tired", "exhausted" -> "😫"
                "stressed" -> "😰"
                "sad", "bad" -> "😢"
                "terrible" -> "😭"
                else -> "😐"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Reflection>() {
        override fun areItemsTheSame(oldItem: Reflection, newItem: Reflection): Boolean {
            return oldItem.reflectionId == newItem.reflectionId
        }

        override fun areContentsTheSame(oldItem: Reflection, newItem: Reflection): Boolean {
            return oldItem == newItem
        }
    }
}