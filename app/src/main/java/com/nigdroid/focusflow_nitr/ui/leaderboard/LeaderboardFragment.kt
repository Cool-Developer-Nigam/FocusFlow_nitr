package com.nigdroid.focusflow_nitr.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nigdroid.focusflow_nitr.databinding.FragmentLeaderboardBinding
import com.nigdroid.focusflow_nitr.ui.adapter.LeaderboardAdapter
import kotlin.getValue

class LeaderboardFragment : Fragment() {
    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LeaderboardViewModel by viewModels()
    private val adapter = LeaderboardAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        binding.leaderboardRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LeaderboardFragment.adapter
        }
    }

    private fun observeData() {
        viewModel.leaderboard.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
        }

        viewModel.userRank.observe(viewLifecycleOwner) { userEntry ->
            binding.userRankValue.text = "#${userEntry.rank}"
            binding.userHours.text = "${String.format("%.1f", userEntry.hours)}h this week"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
