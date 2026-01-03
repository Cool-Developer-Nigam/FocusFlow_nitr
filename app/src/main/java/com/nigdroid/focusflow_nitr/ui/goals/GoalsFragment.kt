package com.nigdroid.focusflow_nitr.ui.goals

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nigdroid.focusflow_nitr.databinding.FragmentGoalsBinding
import com.nigdroid.focusflow_nitr.ui.adapter.GoalsAdapter
import kotlin.getValue
import com.nigdroid.focusflow_nitr.R

class GoalsFragment : Fragment() {
    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GoalsViewModel by viewModels()

    private lateinit var goalsAdapter: GoalsAdapter
    private lateinit var completedGoalsAdapter: GoalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerViews() {
        goalsAdapter = GoalsAdapter(
            onGoalClick = { goal ->
                // Navigate to goal details
                val intent = Intent(requireContext(), GoalDetailsActivity::class.java)
                intent.putExtra("GOAL_ID", goal.id)
                startActivity(intent)
            },
            onMenuClick = { goal, view ->
                showGoalMenu(goal, view)
            }
        )

        binding.goalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalsAdapter
        }

        completedGoalsAdapter = GoalsAdapter(
            onGoalClick = { goal ->
                val intent = Intent(requireContext(), GoalDetailsActivity::class.java)
                intent.putExtra("GOAL_ID", goal.id)
                startActivity(intent)
            },
            onMenuClick = { goal, view ->
                showCompletedGoalMenu(goal, view)
            }
        )

        binding.completedGoalsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = completedGoalsAdapter
        }
    }

    private fun setupObservers() {
        viewModel.activeGoals.observe(viewLifecycleOwner) { goals ->
            goalsAdapter.submitList(goals)
            binding.emptyGoalsLayout.visibility =
                if (goals.isEmpty()) View.VISIBLE else View.GONE
            binding.goalsRecyclerView.visibility =
                if (goals.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.completedGoals.observe(viewLifecycleOwner) { goals ->
            completedGoalsAdapter.submitList(goals)
            binding.completedGoalsSection.visibility =
                if (goals.isEmpty()) View.GONE else View.VISIBLE
            binding.completedGoalsRecyclerView.visibility =
                if (goals.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingProgress.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.addGoalButton.setOnClickListener {
            startActivity(Intent(requireContext(), CreateGoalActivity::class.java))
        }
    }

    private fun showGoalMenu(goal: com.nigdroid.focusflow_nitr.data.model.Goal, view: View) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.goal_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_complete -> {
                        viewModel.markGoalAsComplete(goal.id)
                        true
                    }
                    R.id.action_delete -> {
                        viewModel.deleteGoal(goal.id)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showCompletedGoalMenu(goal: com.nigdroid.focusflow_nitr.data.model.Goal, view: View) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.completed_goal_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_delete -> {
                        viewModel.deleteGoal(goal.id)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadGoals()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}