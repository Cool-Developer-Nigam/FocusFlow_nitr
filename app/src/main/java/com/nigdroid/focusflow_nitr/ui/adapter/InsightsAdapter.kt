package com.nigdroid.focusflow_nitr.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nigdroid.focusflow_nitr.databinding.ItemInsightBinding

class InsightsAdapter(private val insights: List<String>) :
    RecyclerView.Adapter<InsightsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInsightBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(insights[position])
    }

    override fun getItemCount() = insights.size

    class ViewHolder(private val binding: ItemInsightBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(insight: String) {
            binding.insightText.text = insight
        }
    }
}