package com.nigdroid.focusflow_nitr.ui.goals

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nigdroid.focusflow_nitr.R
import com.nigdroid.focusflow_nitr.data.local.PreferencesManager
import com.nigdroid.focusflow_nitr.data.model.Goal
import com.nigdroid.focusflow_nitr.databinding.ActivityCreateGoalBinding
import com.nigdroid.focusflow_nitr.ui.main.MainActivity
import com.nigdroid.focusflow_nitr.utils.toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CreateGoalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateGoalBinding
    private lateinit var prefsManager: PreferencesManager
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedDeadline: Long = 0
    private val selectedApps = mutableListOf<String>()

    // School-friendly productive apps for students
    private val productiveApps = listOf(
        // Learning & Education
        "Unacademy","Physics Wallah","Khan Academy", "Duolingo", "Photomath", "Brainly",
        "Quizlet", "Google Classroom", "Byju's",

        // Study Tools
        "Notion", "Evernote", "Google Keep", "Microsoft OneNote",
        "Forest - Focus Timer", "Study Bunny", "My Study Life",

        // Subject Specific
        "Chemistry Lab", "Physics Toolbox", "GeoGebra",
        "WolframAlpha", "Google Translate", "Dictionary.com",

        // Reading & Books
        "Kindle", "Google Books", "Audible", "Scribd",

        // Communication (School)
        "Google Meet", "Zoom", "Microsoft Teams",

        // Productivity
        "Google Docs", "Google Drive", "Todoist", "Google Calendar","Google Chrome"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager(this)

        setupToolbar()
        setupCategoryDropdown()
        setupProductiveAppsChips()
        setupDeadlinePicker()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupCategoryDropdown() {
        // School-friendly categories with emojis
        val categories = arrayOf(
            // Core Subjects
            "📚 Mathematics",
            "🔬 Science - Physics",
            "🔬 Science - Chemistry",
            "🔬 Science - Biology",
            "💻 Computer Science",
            "🌍 Social Studies/History",
            "🗣️ English/Language Arts",
            "🌏 Foreign Language",

            // Skills & Activities
            "🎨 Art & Creativity",
            "🎵 Music",
            "⚽ Sports & Fitness",
            "📖 Reading & Literature",

            // Exams & Tests
            "📝 Exam Preparation",
            "🎯 Assignment/Project Work",
            "📊 Test Practice",

            // Personal Development
            "🧠 Memory & Focus Training",
            "✍️ Writing Skills",
            "🗨️ Public Speaking",
            "💡 Problem Solving",

            // Other
            "🌟 Other"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        (binding.categoryInput as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupProductiveAppsChips() {
        binding.productiveAppsChipGroup.removeAllViews()

        productiveApps.forEach { appName ->
            val chip = Chip(this).apply {
                text = appName
                isCheckable = true
                setChipBackgroundColorResource(R.color.background_secondary)
                setTextColor(getColor(R.color.text_primary))
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedApps.add(appName)
                    } else {
                        selectedApps.remove(appName)
                    }
                }
            }
            binding.productiveAppsChipGroup.addView(chip)
        }
    }

    private fun setupDeadlinePicker() {
        binding.deadlineInput.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                showDatePicker()
            }
        }

        binding.deadlineLayout.setEndIconOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Set minimum date to tomorrow
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val minDate = calendar.timeInMillis

        // Reset to today for default selection
        calendar.add(Calendar.DAY_OF_MONTH, -1)

        // Default to 2 weeks from now (more realistic for school goals)
        calendar.add(Calendar.WEEK_OF_YEAR, 2)

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

        datePickerDialog.datePicker.minDate = minDate
        datePickerDialog.show()
    }

    private fun setupListeners() {
        binding.createButton.setOnClickListener {
            validateAndCreateGoal()
        }

        binding.skipButton.setOnClickListener {
            // Check if this is first goal setup
            val isFirstGoal = intent.getBooleanExtra("IS_FIRST_GOAL", false)
            if (isFirstGoal) {
                // Skip to main activity
                prefsManager.hasCreatedGoal = true
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // Just close this activity
                finish()
            }
        }
    }

    private fun validateAndCreateGoal() {
        // Clear previous errors
        binding.titleLayout.error = null
        binding.categoryLayout.error = null
        binding.targetHoursLayout.error = null
        binding.deadlineLayout.error = null

        val title = binding.titleInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val category = binding.categoryInput.text.toString().trim()
        val targetHoursStr = binding.targetHoursInput.text.toString().trim()

        // Validation
        var hasError = false

        if (title.isEmpty()) {
            binding.titleLayout.error = "Goal title is required"
            hasError = true
        }

        if (category.isEmpty()) {
            binding.categoryLayout.error = "Please select a category"
            hasError = true
        }

        if (targetHoursStr.isEmpty()) {
            binding.targetHoursLayout.error = "Study hours target is required"
            hasError = true
        }

        val targetHours = targetHoursStr.toIntOrNull()
        if (targetHours == null || targetHours <= 0) {
            binding.targetHoursLayout.error = "Enter valid study hours (e.g., 10)"
            hasError = true
        } else if (targetHours > 500) {
            binding.targetHoursLayout.error = "That's too many hours! Try a smaller goal"
            hasError = true
        }

        if (selectedDeadline == 0L) {
            binding.deadlineLayout.error = "Please select a deadline"
            hasError = true
        }

        if (hasError) {
            return
        }

        // Show loading
        binding.createButton.isEnabled = false
        binding.createButton.text = "Creating your goal..."

        // Create goal
        createGoal(title, description, category, targetHours!!, selectedDeadline)
    }

    private fun createGoal(
        title: String,
        description: String,
        category: String,
        targetHours: Int,
        deadline: Long
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            toast("Please login first")
            finish()
            return
        }

        val goalId = firestore.collection("goals").document().id

        val goal = Goal(
            id = goalId,
            userId = userId,
            title = title,
            description = description,
            category = category,
            targetHours = targetHours,
            deadline = deadline,
            isActive = true,
            isCompleted = false,
            createdAt = Timestamp.now(),
            productiveApps = selectedApps
        )

        firestore.collection("goals")
            .document(goalId)
            .set(goal)
            .addOnSuccessListener {
                toast("Goal created successfully! Let's crush it! 🎯")

                // Mark that user has created a goal
                prefsManager.hasCreatedGoal = true

                // Check if this was first goal setup
                val isFirstGoal = intent.getBooleanExtra("IS_FIRST_GOAL", false)
                if (isFirstGoal) {
                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // Return to previous screen
                    setResult(RESULT_OK)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                toast("Oops! Couldn't create goal: ${e.message}")
                binding.createButton.isEnabled = true
                binding.createButton.text = "Create Goal"
            }
    }
}