package com.nigdroid.focusflow_nitr.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.data.local.PreferencesManager
import com.nigdroid.focusflow_nitr.data.repository.UserRepository
import com.nigdroid.focusflow_nitr.databinding.FragmentProfileBinding
import com.nigdroid.focusflow_nitr.ui.adapter.AchievementsAdapter
import com.nigdroid.focusflow_nitr.ui.adapter.ReflectionsAdapter
import com.nigdroid.focusflow_nitr.ui.auth.LoginActivity
import com.nigdroid.focusflow_nitr.utils.gone
import com.nigdroid.focusflow_nitr.utils.toast
import com.nigdroid.focusflow_nitr.utils.visible

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefsManager: PreferencesManager
    private val userRepository = UserRepository()
    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var achievementsAdapter: AchievementsAdapter
    private lateinit var reflectionsAdapter: ReflectionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsManager = PreferencesManager(requireContext())

        setupEmojiLabels()
        setupRecyclerViews()
        setupListeners()
        observeData()
        loadFirebaseUserPhoto()

        // Force load data
        viewModel.refreshProfile()
    }

    private fun loadFirebaseUserPhoto() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val photoUrl = user.photoUrl?.toString()
            val displayName = user.displayName
            val email = user.email

            if (displayName != null) {
                binding.userName.text = displayName
            }
            if (email != null) {
                binding.userEmail.text = email
            }

            if (!photoUrl.isNullOrEmpty()) {
                loadProfileImage(photoUrl)
            }
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(binding.profileImage)
    }

    private fun setupEmojiLabels() {
        binding.streakLabel.text = "🔥 Streak"
        binding.pointsLabel.text = "🪙 Points"
        binding.levelLabel.text = "⭐ Level"
        binding.totalLabel.text = "⏰ Total"
        binding.achievementsTitle.text = "🏅 Achievements"
        binding.reflectionsTitle.text = "📝 Daily Reflections"
    }

    private fun setupRecyclerViews() {
        // Achievements RecyclerView
        achievementsAdapter = AchievementsAdapter()
        binding.achievementsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = achievementsAdapter
            setHasFixedSize(true)
        }

        // Reflections RecyclerView - FIXED
        reflectionsAdapter = ReflectionsAdapter { reflection ->
            showDeleteConfirmation(reflection.reflectionId)
        }
        binding.reflectionsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reflectionsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.logoutButton.setOnClickListener {
            logout()
        }

        binding.addReflectionButton.setOnClickListener {
            showAddReflectionDialog()
        }

        binding.profileImage.setOnClickListener {
            toast("Profile photo from Google Account")
        }
    }

    private fun observeData() {
        // User data
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                if (binding.userName.text.isEmpty() || binding.userName.text == "Rahul Kumar") {
                    binding.userName.text = it.name
                }
                if (binding.userEmail.text.isEmpty() || binding.userEmail.text == "rahul@example.com") {
                    binding.userEmail.text = it.email
                }

                binding.streakValue.text = "${it.streak} days"
                binding.pointsValue.text = "${it.points} pts"
                binding.levelValue.text = "Level ${it.level}"
                binding.totalHoursValue.text = "${String.format("%.1f", it.totalFocusHours)}h"

                if (it.avatarUrl.isNotEmpty()) {
                    loadProfileImage(it.avatarUrl)
                }
            }
        }

        // Achievements
        viewModel.achievements.observe(viewLifecycleOwner) { achievements ->
            android.util.Log.d("ProfileFragment", "Achievements updated: ${achievements.size}")
            achievementsAdapter.submitList(achievements)

            val unlockedCount = achievements.count { it.isUnlocked }
            binding.achievementsCount.text = "$unlockedCount/${achievements.size} Unlocked"
        }

        // Reflections - FIXED
        viewModel.reflections.observe(viewLifecycleOwner) { reflections ->
            android.util.Log.d("ProfileFragment", "Reflections updated: ${reflections.size}")

            if (reflections.isEmpty()) {
                binding.reflectionsRecycler.gone()
                binding.emptyReflectionsText.visible()
                binding.emptyReflectionsText.text = "No reflections yet. Add your first reflection!"
            } else {
                binding.reflectionsRecycler.visible()
                binding.emptyReflectionsText.gone()
                reflectionsAdapter.submitList(reflections.toList()) // Convert to new list
            }

            binding.reflectionsCount.text = "${reflections.size} reflections"
        }

        // Loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressBar.visible()
            } else {
                binding.progressBar.gone()
            }
        }
    }

    private fun showAddReflectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reflection, null)
        val moodButtons = listOf(
            dialogView.findViewById<View>(R.id.moodGreat),
            dialogView.findViewById<View>(R.id.moodGood),
            dialogView.findViewById<View>(R.id.moodOkay),
            dialogView.findViewById<View>(R.id.moodTired),
            dialogView.findViewById<View>(R.id.moodBad)
        )
        val noteInput = dialogView.findViewById<TextInputEditText>(R.id.noteInput)

        var selectedMood = "okay"

        moodButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                moodButtons.forEach { it.alpha = 0.5f }
                button.alpha = 1.0f

                selectedMood = when (index) {
                    0 -> "great"
                    1 -> "good"
                    2 -> "okay"
                    3 -> "tired"
                    4 -> "bad"
                    else -> "okay"
                }
            }
        }

        moodButtons[2].alpha = 1.0f

        AlertDialog.Builder(requireContext())
            .setTitle("Add Today's Reflection")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val note = noteInput.text?.toString() ?: ""
                if (note.isNotBlank()) {
                    viewModel.addReflection(selectedMood, note)
                    toast("Reflection saved!")
                } else {
                    toast("Please write a note")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(reflectionId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Reflection?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteReflection(reflectionId)
                toast("Reflection deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                userRepository.logout()
                prefsManager.clear()
                prefsManager.isLoggedIn = false
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadFirebaseUserPhoto()
        viewModel.refreshProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}