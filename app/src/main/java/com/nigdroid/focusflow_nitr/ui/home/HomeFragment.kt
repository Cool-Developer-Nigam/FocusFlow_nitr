package com.nigdroid.focusflow_nitr.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nigdroid.focusflow_nitr.databinding.FragmentHomeBinding
import com.nigdroid.focusflow_nitr.ui.adapter.SessionAdapter
import com.nigdroid.focusflow_nitr.ui.focus.FocusSessionActivity
import com.nigdroid.focusflow_nitr.utils.DateUtils
import com.nigdroid.focusflow_nitr.utils.PermissionHelper

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var sessionAdapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        observeData()
        checkPermissions()

        // Force refresh data
        viewModel.refreshData()
    }

    private fun setupUI() {
        binding.greetingText.text = DateUtils.getGreeting()
        binding.dateText.text = DateUtils.getCurrentDayName()
    }

    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter()
        binding.recentActivityRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeData() {
        // Observe focus energy
        viewModel.focusEnergy.observe(viewLifecycleOwner) { energy ->
            android.util.Log.d("HomeFragment", "Focus energy updated: $energy")
            binding.energyProgress.progress = energy
            binding.energyValue.text = "$energy/100"
        }

        // Observe today's hours
        viewModel.todayHours.observe(viewLifecycleOwner) { (current, target) ->
            android.util.Log.d("HomeFragment", "Today's hours updated: $current / $target")

            val currentFormatted = String.format("%.1f", current)
            val targetFormatted = String.format("%.0f", target)

            binding.progressHours.text = "${currentFormatted}h / ${targetFormatted}h"

            val percentage = if (target > 0) {
                ((current / target) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            binding.dailyProgress.progress = percentage
            binding.progressPercent.text = "$percentage%"
        }

        // Observe streak
        viewModel.streak.observe(viewLifecycleOwner) { streak ->
            android.util.Log.d("HomeFragment", "Streak updated: $streak")
            binding.streakValue.text = "$streak days"
        }

        // Observe goals count
        viewModel.goals.observe(viewLifecycleOwner) { goals ->
            android.util.Log.d("HomeFragment", "Goals updated: ${goals.size}")
            binding.activeGoalsCount.text = goals.size.toString()
        }

        // Observe recent sessions
        viewModel.recentSessions.observe(viewLifecycleOwner) { sessions ->
            android.util.Log.d("HomeFragment", "Recent sessions updated: ${sessions.size}")

            if (sessions.isEmpty()) {
                binding.emptyRecentActivity.visibility = View.VISIBLE
                binding.recentActivityRecycler.visibility = View.GONE
                binding.emptyRecentActivity.text = "No recent focus sessions yet.\n\nTap the + button below to start your first session!"
            } else {
                binding.emptyRecentActivity.visibility = View.GONE
                binding.recentActivityRecycler.visibility = View.VISIBLE
                sessionAdapter.submitList(sessions)
            }
        }

        // Observe fatigue
        viewModel.fatigue.observe(viewLifecycleOwner) { fatigueScore ->
            android.util.Log.d("HomeFragment", "Fatigue updated: $fatigueScore")

            val level = when {
                fatigueScore <= 30 -> "🟢 Low"
                fatigueScore <= 60 -> "🟡 Moderate"
                fatigueScore <= 80 -> "🟠 High"
                else -> "🔴 Critical"
            }
            binding.fatigueValue.text = "$fatigueScore/100 $level"
        }

        // Observe fatigue recommendations
        viewModel.fatigueRecommendations.observe(viewLifecycleOwner) { recommendations ->
            if (recommendations.isNotEmpty()) {
                binding.fatigueRecommendation.text = "💡 ${recommendations.first()}"
            }
        }
    }

    private fun checkPermissions() {
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(requireContext())
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(requireContext())

        if (!hasUsageStats || !hasAccessibility) {
            val message = when {
                !hasUsageStats && !hasAccessibility ->
                    "Grant permissions to track your focus time"
                !hasUsageStats ->
                    "Enable screen time tracking for insights"
                else ->
                    "Enable typing monitor for better tracking"
            }

            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction("Enable") {
                    (requireActivity() as? AppCompatActivity)?.let { activity ->
                        PermissionHelper.checkAndRequestAllPermissions(activity)
                    }
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("HomeFragment", "onResume - refreshing data")
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}