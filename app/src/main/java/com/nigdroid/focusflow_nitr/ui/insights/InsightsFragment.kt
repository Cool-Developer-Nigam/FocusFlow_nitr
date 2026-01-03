package com.nigdroid.focusflow_nitr.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.tabs.TabLayout
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.databinding.FragmentInsightsBinding
import com.nigdroid.focusflow_nitr.ui.adapter.InsightsAdapter

class InsightsFragment : Fragment() {
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InsightsViewModel by viewModels()

    private var currentPeriod = InsightsPeriod.WEEK

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)

        // Set ViewModel for data binding
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

        // Load initial data for WEEK tab
        viewModel.loadInsightsForPeriod(InsightsPeriod.WEEK)
    }

    private fun setupRecyclerViews() {
        binding.insightsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        binding.predictionsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupChart() {
        val chart = binding.weeklyChart

        chart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setMaxVisibleValueCount(60)
            setPinchZoom(false)
            setTouchEnabled(true)

            // Axis configuration
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = requireContext().getColor(R.color.glass_border)
                textColor = requireContext().getColor(R.color.text_secondary)
                axisMinimum = 0f
                granularity = 0.5f
            }

            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = requireContext().getColor(R.color.text_secondary)
                textSize = 10f
            }

            legend.apply {
                textColor = requireContext().getColor(R.color.text_primary)
                textSize = 12f
                isEnabled = true
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            }

            setExtraOffsets(0f, 0f, 0f, 10f)
        }
    }

    private fun setupTabs() {
        // Set default selected tab (WEEK = index 1)
        binding.tabLayout.getTabAt(1)?.select()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val period = when (it.position) {
                        0 -> {
                            android.util.Log.d("InsightsFragment", "TODAY tab selected")
                            InsightsPeriod.TODAY
                        }
                        1 -> {
                            android.util.Log.d("InsightsFragment", "WEEK tab selected")
                            InsightsPeriod.WEEK
                        }
                        2 -> {
                            android.util.Log.d("InsightsFragment", "MONTH tab selected")
                            InsightsPeriod.MONTH
                        }
                        else -> InsightsPeriod.WEEK
                    }

                    currentPeriod = period

                    // Show loading
                    binding.loadingProgress.visibility = View.VISIBLE

                    // Load data for selected period
                    viewModel.loadInsightsForPeriod(period)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Reload data on tab reselect
                tab?.let {
                    val period = when (it.position) {
                        0 -> InsightsPeriod.TODAY
                        1 -> InsightsPeriod.WEEK
                        2 -> InsightsPeriod.MONTH
                        else -> InsightsPeriod.WEEK
                    }
                    android.util.Log.d("InsightsFragment", "Tab ${period.name} reselected - reloading data")
                    viewModel.loadInsightsForPeriod(period)
                }
            }
        })
    }

    private fun observeData() {
        // Observe insights data
        viewModel.insights.observe(viewLifecycleOwner) { insights ->
            android.util.Log.d("InsightsFragment", "Insights received: focusScore=${insights.focusScore}, period=$currentPeriod")

            // Update focus score
            binding.focusScoreValue.text = "${insights.focusScore}/100"

            // Update score change indicator
            val change = insights.weeklyComparison.changePercentage
            val changeText = if (change >= 0) "+$change%" else "$change%"
            val periodName = when (currentPeriod) {
                InsightsPeriod.TODAY -> "from yesterday"
                InsightsPeriod.WEEK -> "from last week"
                InsightsPeriod.MONTH -> "from last month"
            }
            binding.focusScoreChange.text = "$changeText $periodName"
            binding.focusScoreChange.setTextColor(
                if (change >= 0)
                    requireContext().getColor(R.color.success_green)
                else
                    requireContext().getColor(R.color.error_red)
            )

            // Update recommendations
            if (insights.recommendations.isNotEmpty()) {
                android.util.Log.d("InsightsFragment", "Showing ${insights.recommendations.size} recommendations")
                binding.insightsRecycler.adapter = InsightsAdapter(insights.recommendations)
            } else {
                android.util.Log.d("InsightsFragment", "No recommendations, showing defaults")
                binding.insightsRecycler.adapter = InsightsAdapter(getDefaultRecommendations())
            }

            // Update predictions
            if (insights.predictions.isNotEmpty()) {
                android.util.Log.d("InsightsFragment", "Showing ${insights.predictions.size} predictions")
                binding.predictionsRecycler.adapter = InsightsAdapter(insights.predictions)
            } else {
                android.util.Log.d("InsightsFragment", "No predictions, showing defaults")
                binding.predictionsRecycler.adapter = InsightsAdapter(getDefaultPredictions())
            }
        }

        // Observe chart data
        viewModel.chartData.observe(viewLifecycleOwner) { chartData ->
            android.util.Log.d("InsightsFragment", "Chart data received: ${chartData.size} points for period: $currentPeriod")
            updateChart(chartData)
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            android.util.Log.d("InsightsFragment", "Loading state changed: $isLoading")
            binding.loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun updateChart(chartData: List<DailyData>) {
        val chart = binding.weeklyChart

        android.util.Log.d("InsightsFragment", "Updating chart with ${chartData.size} data points for period: $currentPeriod")

        if (chartData.isEmpty()) {
            android.util.Log.w("InsightsFragment", "Chart data is empty, clearing chart")
            chart.clear()
            chart.invalidate()
            return
        }

        try {
            // Create bar entries
            val entries = chartData.mapIndexed { index, dailyData ->
                android.util.Log.v("InsightsFragment", "Chart entry $index: ${dailyData.dayName} = ${dailyData.hours}h")
                BarEntry(index.toFloat(), dailyData.hours.toFloat())
            }

            // Create dataset with dynamic label
            val datasetLabel = when (currentPeriod) {
                InsightsPeriod.TODAY -> "Hours Today"
                InsightsPeriod.WEEK -> "Hours This Week"
                InsightsPeriod.MONTH -> "Hours This Month"
            }

            val dataSet = BarDataSet(entries, datasetLabel).apply {
                color = requireContext().getColor(R.color.purple_primary)
                valueTextColor = requireContext().getColor(R.color.text_primary)
                valueTextSize = 10f
                setDrawValues(true)
            }

            // Create bar data
            val barData = BarData(dataSet).apply {
                barWidth = when (currentPeriod) {
                    InsightsPeriod.TODAY -> 0.7f
                    InsightsPeriod.WEEK -> 0.8f
                    InsightsPeriod.MONTH -> 0.85f
                }
            }

            // Configure X-axis labels
            chart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(chartData.map { it.dayName })
                labelCount = chartData.size
                granularity = 1f

                // Adjust label rotation for TODAY (many labels)
                if (currentPeriod == InsightsPeriod.TODAY) {
                    labelRotationAngle = -45f
                    setLabelCount(chartData.size, false)
                } else {
                    labelRotationAngle = 0f
                    setLabelCount(chartData.size, true)
                }
            }

            // Set data and refresh
            chart.data = barData
            chart.setFitBars(true)
            chart.animateY(800)
            chart.notifyDataSetChanged()
            chart.invalidate()

            android.util.Log.d("InsightsFragment", "Chart updated successfully for period: $currentPeriod")
        } catch (e: Exception) {
            android.util.Log.e("InsightsFragment", "Error updating chart", e)
        }
    }

    private fun getDefaultRecommendations(): List<String> {
        return listOf(
            "💡 Start tracking your focus sessions to get personalized insights",
            "📊 Complete at least 5 sessions to unlock detailed analytics",
            "🎯 Set daily goals to improve your productivity"
        )
    }

    private fun getDefaultPredictions(): List<String> {
        return listOf(
            "🔮 Complete more sessions to unlock AI-powered predictions",
            "📈 Your productivity patterns will be analyzed after 7 days",
            "⚡ Keep building your streak for better insights"
        )
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("InsightsFragment", "onResume - refreshing data for period: $currentPeriod")
        // Refresh data when fragment becomes visible
        viewModel.loadInsightsForPeriod(currentPeriod)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}