package com.nigdroid.focusflow_nitr.ui.goals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.data.model.Goal
import com.nigdroid.focusflow_nitr.databinding.ActivityGoalDetailsBinding
import com.nigdroid.focusflow_nitr.utils.toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoalDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoalDetailsBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentGoal: Goal? = null
    private var isEditMode = false
    private var selectedDeadline: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadGoalDetails()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (isEditMode) {
                showDiscardChangesDialog()
            } else {
                finish()
            }
        }
    }

    private fun loadGoalDetails() {
        val goalId = intent.getStringExtra("GOAL_ID")
        if (goalId == null) {
            toast("Goal not found")
            finish()
            return
        }

        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.detailsCard.visibility = View.GONE

        firestore.collection("goals")
            .document(goalId)
            .get()
            .addOnSuccessListener { document ->
                val goal = document.toObject(Goal::class.java)?.copy(id = document.id)
                if (goal != null) {
                    currentGoal = goal
                    displayGoalDetails(goal)
                } else {
                    toast("Goal not found")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                toast("Failed to load goal: ${e.message}")
                finish()
            }
            .addOnCompleteListener {
                binding.loadingProgressBar.visibility = View.GONE
                binding.detailsCard.visibility = View.VISIBLE
            }
    }

    private fun displayGoalDetails(goal: Goal) {
        // Set status chip
        if (goal.isCompleted) {
            binding.statusChip.text = "Completed"
            binding.statusChip.setChipBackgroundColorResource(R.color.success_green)
        } else if (goal.deadline < System.currentTimeMillis()) {
            binding.statusChip.text = "Overdue"
            binding.statusChip.setChipBackgroundColorResource(R.color.error_red)
        } else {
            binding.statusChip.text = "Active"
            binding.statusChip.setChipBackgroundColorResource(R.color.purple_primary)
        }

        // Fill in details
        binding.titleInput.setText(goal.title)
        binding.descriptionInput.setText(goal.description)
        binding.targetHoursInput.setText(goal.targetHours.toString())
        binding.categoryInput.setText(goal.category)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.deadlineInput.setText(dateFormat.format(Date(goal.deadline)))
        selectedDeadline = goal.deadline

        // Set progress
        val progressHours = TimeUnit.MILLISECONDS.toHours(goal.currentProgress).toInt()
        val percentage = ((progressHours.toFloat() / goal.targetHours) * 100).toInt().coerceAtMost(100)

        binding.goalProgressBar.max = 100
        binding.goalProgressBar.progress = percentage
        binding.progressText.text = "$progressHours / ${goal.targetHours} hours"
        binding.progressPercentage.text = "$percentage%"

        // Calculate days left
        val daysLeft = TimeUnit.MILLISECONDS.toDays(goal.deadline - System.currentTimeMillis())
        binding.daysLeftText.text = when {
            daysLeft < 0 -> "Overdue"
            daysLeft == 0L -> "Due today"
            daysLeft == 1L -> "1 day left"
            else -> "$daysLeft days left"
        }

        // Created date
        binding.createdDateText.text = dateFormat.format(goal.createdAt.toDate())

        // Display productive apps
        displayProductiveApps(goal.productiveApps)

        // Update button visibility
        updateButtonVisibility(goal)
    }

    private fun displayProductiveApps(apps: List<String>) {
        binding.productiveAppsChipGroup.removeAllViews()

        if (apps.isEmpty()) {
            binding.appsLabel.visibility = View.GONE
            binding.productiveAppsChipGroup.visibility = View.GONE
            return
        }

        binding.appsLabel.visibility = View.VISIBLE
        binding.productiveAppsChipGroup.visibility = View.VISIBLE

        apps.forEach { appName ->
            val chip = Chip(this).apply {
                text = appName
                isClickable = false
                setChipBackgroundColorResource(R.color.background_dark)
                setTextColor(getColor(R.color.text_primary))
            }
            binding.productiveAppsChipGroup.addView(chip)
        }
    }

    private fun updateButtonVisibility(goal: Goal) {
        if (goal.isCompleted) {
            binding.completeButton.visibility = View.GONE
            binding.editButton.visibility = View.GONE
            binding.saveButton.visibility = View.GONE
        } else {
            binding.completeButton.visibility = View.VISIBLE
            binding.editButton.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.editButton.setOnClickListener {
            toggleEditMode()
        }

        binding.saveButton.setOnClickListener {
            saveChanges()
        }

        binding.completeButton.setOnClickListener {
            showCompleteGoalDialog()
        }

        binding.deleteButton.setOnClickListener {
            showDeleteGoalDialog()
        }

        binding.deadlineInput.setOnClickListener {
            if (isEditMode) {
                showDatePicker()
            }
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        // Enable/disable inputs
        binding.titleInput.isEnabled = isEditMode
        binding.descriptionInput.isEnabled = isEditMode
        binding.targetHoursInput.isEnabled = isEditMode
        binding.categoryInput.isEnabled = isEditMode
        binding.deadlineInput.isEnabled = isEditMode

        // Show/hide buttons
        binding.editButton.visibility = if (isEditMode) View.GONE else View.VISIBLE
        binding.saveButton.visibility = if (isEditMode) View.VISIBLE else View.GONE

        if (isEditMode) {
            toast("Edit mode enabled")
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDeadline

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 23, 59, 59)
                selectedDeadline = selectedCalendar.timeInMillis

                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                binding.deadlineInput.setText(dateFormat.format(Date(selectedDeadline)))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun saveChanges() {
        val goal = currentGoal ?: return

        val title = binding.titleInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val targetHoursStr = binding.targetHoursInput.text.toString().trim()
        val category = binding.categoryInput.text.toString().trim()

        if (title.isEmpty() || targetHoursStr.isEmpty() || category.isEmpty()) {
            toast("Please fill in all required fields")
            return
        }

        val targetHours = targetHoursStr.toIntOrNull()
        if (targetHours == null || targetHours <= 0) {
            toast("Please enter a valid number of hours")
            return
        }

        binding.saveButton.isEnabled = false
        binding.saveButton.text = "Saving..."

        val updates = mapOf(
            "title" to title,
            "description" to description,
            "targetHours" to targetHours,
            "category" to category,
            "deadline" to selectedDeadline
        )

        firestore.collection("goals")
            .document(goal.id)
            .update(updates)
            .addOnSuccessListener {
                toast("Goal updated successfully")
                toggleEditMode()
                loadGoalDetails() // Reload to show updated data
            }
            .addOnFailureListener { e ->
                toast("Failed to update goal: ${e.message}")
            }
            .addOnCompleteListener {
                binding.saveButton.isEnabled = true
                binding.saveButton.text = "Save Changes"
            }
    }

    private fun showCompleteGoalDialog() {
        AlertDialog.Builder(this)
            .setTitle("Complete Goal?")
            .setMessage("Are you sure you want to mark this goal as complete? You can't undo this action.")
            .setPositiveButton("Complete") { _, _ ->
                completeGoal()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeGoal() {
        val goal = currentGoal ?: return

        firestore.collection("goals")
            .document(goal.id)
            .update(
                mapOf(
                    "isCompleted" to true,
                    "completedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                toast("🎉 Goal completed! Great work!")
                finish()
            }
            .addOnFailureListener { e ->
                toast("Failed to complete goal: ${e.message}")
            }
    }

    private fun showDeleteGoalDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Goal?")
            .setMessage("Are you sure you want to delete this goal? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteGoal()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGoal() {
        val goal = currentGoal ?: return

        firestore.collection("goals")
            .document(goal.id)
            .delete()
            .addOnSuccessListener {
                toast("Goal deleted")
                finish()
            }
            .addOnFailureListener { e ->
                toast("Failed to delete goal: ${e.message}")
            }
    }

    private fun showDiscardChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved changes. Do you want to discard them?")
            .setPositiveButton("Discard") { _, _ ->
                finish()
            }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    override fun onBackPressed() {
        if (isEditMode) {
            showDiscardChangesDialog()
        } else {
            super.onBackPressed()
        }
    }
}