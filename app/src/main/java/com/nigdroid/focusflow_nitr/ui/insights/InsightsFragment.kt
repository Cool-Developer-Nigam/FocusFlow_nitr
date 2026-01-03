package com.nigdroid.focusflow_nitr.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.tabs.TabLayout
import com.nigdroid.focusflow_nitr.databinding.FragmentInsightsBinding
import com.nigdroid.focusflow_nitr.ui.adapter.InsightsAdapter
import com.nigdroid.focusflow_nitr.R
import kotlin.getValue


class InsightsFragment : Fragment() {
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InsightsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupChart()
        setupTabs()
        observeData()
    }

    private fun setupRecyclerViews() {
        binding.insightsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.predictionsRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupChart() {
        updateChart(emptyList())
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadInsightsForPeriod(InsightsPeriod.TODAY)
                    1 -> viewModel.loadInsightsForPeriod(InsightsPeriod.WEEK)
                    2 -> viewModel.loadInsightsForPeriod(InsightsPeriod.MONTH)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeData() {
        viewModel.insights.observe(viewLifecycleOwner) { insights ->
            binding.focusScoreValue.text = "${insights.focusScore}/100"

            val change = if (insights.focusScore > 50) "+${insights.focusScore - 50}%" else "${insights.focusScore - 50}%"
            binding.focusScoreChange.text = "$change from last period"
            binding.focusScoreChange.setTextColor(
                if (insights.focusScore > 50)
                    requireContext().getColor(R.color.success_green)
                else
                    requireContext().getColor(R.color.error_red)
            )

            if (insights.recommendations.isNotEmpty()) {
                binding.insightsRecycler.adapter = InsightsAdapter(insights.recommendations)
            } else {
                binding.insightsRecycler.adapter = InsightsAdapter(
                    listOf(
                        "💡 Start tracking your focus sessions to get personalized insights",
                        "📊 Complete at least 5 sessions to unlock detailed analytics",
                        "🎯 Set daily goals to improve your productivity"
                    )
                )
            }

            if (insights.predictions.isNotEmpty()) {
                binding.predictionsRecycler.adapter = InsightsAdapter(insights.predictions)
            } else {
                binding.predictionsRecycler.adapter = InsightsAdapter(
                    listOf(
                        "🔮 Complete more sessions to unlock AI-powered predictions",
                        "📈 Your productivity patterns will be analyzed after 7 days",
                        "⚡ Keep building your streak for better insights"
                    )
                )
            }
        }

        viewModel.chartData.observe(viewLifecycleOwner) { chartData ->
            updateChart(chartData)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun updateChart(chartData: List<DailyData>) {
        val barChart = binding.weeklyChart

        if (chartData.isEmpty()) {
            val placeholderData = listOf(
                DailyData("Mon", 0.0, 0),
                DailyData("Tue", 0.0, 0),
                DailyData("Wed", 0.0, 0),
                DailyData("Thu", 0.0, 0),
                DailyData("Fri", 0.0, 0),
                DailyData("Sat", 0.0, 0),
                DailyData("Sun", 0.0, 0)
            )
            setupBarChart(barChart, placeholderData)
        } else {
            setupBarChart(barChart, chartData)
        }
    }

    private fun setupBarChart(chart: BarChart, data: List<DailyData>) {
        val entries = data.mapIndexed { index, dailyData ->
            BarEntry(index.toFloat(), dailyData.hours.toFloat())
        }

        val dataSet = BarDataSet(entries, "Focus Hours").apply {
            color = requireContext().getColor(R.color.purple_primary)
            valueTextColor = requireContext().getColor(R.color.text_primary)
            valueTextSize = 12f
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        chart.apply {
            this.data = barData
            description.isEnabled = false
            setFitBars(true)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(data.map { it.dayName })
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = requireContext().getColor(R.color.text_secondary)
            }

            axisLeft.apply {
                textColor = requireContext().getColor(R.color.text_secondary)
                setDrawGridLines(true)
                gridColor = requireContext().getColor(R.color.glass_border)
                axisMinimum = 0f
            }

            axisRight.isEnabled = false

            legend.apply {
                textColor = requireContext().getColor(R.color.text_primary)
                textSize = 12f
            }

            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}