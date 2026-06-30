package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Cat
import com.example.data.CatRepository
import com.example.data.GeminiService
import com.example.data.TrackingLog
import com.example.data.Reminder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface AiAdviceState {
    object Idle : AiAdviceState
    object Loading : AiAdviceState
    data class Success(val advice: String) : AiAdviceState
    data class Error(val message: String) : AiAdviceState
    object MissingKey : AiAdviceState
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatTrackerViewModel(private val repository: CatRepository) : ViewModel() {

    // List of all cats
    val allCats: StateFlow<List<Cat>> = repository.allCats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected cat ID
    private val _selectedCatId = MutableStateFlow<Int?>(null)
    val selectedCatId: StateFlow<Int?> = _selectedCatId.asStateFlow()

    // Automatically select the first cat if none is selected
    val selectedCat: StateFlow<Cat?> = combine(allCats, selectedCatId) { cats, id ->
        if (id != null) {
            cats.find { it.id == id }
        } else if (cats.isNotEmpty()) {
            _selectedCatId.value = cats.first().id
            cats.first()
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current logging date (YYYY-MM-DD)
    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Formatted display date (e.g. "June 29, 2026")
    val displayDate: StateFlow<String> = _selectedDate.map { dateStr ->
        try {
            val localDate = LocalDate.parse(dateStr)
            localDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        } catch (e: Exception) {
            dateStr
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Daily logs for the selected cat on the selected date
    val dailyLogs: StateFlow<List<TrackingLog>> = combine(selectedCatId, _selectedDate) { catId, date ->
        catId to date
    }.flatMapLatest { (catId, date) ->
        if (catId != null) {
            repository.getDailyLogsForCat(catId, date)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders for the selected cat
    val reminders: StateFlow<List<Reminder>> = selectedCatId.flatMapLatest { catId ->
        if (catId != null) {
            repository.getRemindersForCat(catId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI advice status state
    private val _aiAdviceState = MutableStateFlow<AiAdviceState>(AiAdviceState.Idle)
    val aiAdviceState: StateFlow<AiAdviceState> = _aiAdviceState.asStateFlow()

    // Add a new cat
    fun addCat(name: String, breed: String, ageMonths: Int, weightKg: Double, emoji: String) {
        viewModelScope.launch {
            val newCat = Cat(
                name = name,
                breed = breed.ifBlank { "Domestic Shorthair" },
                ageMonths = ageMonths.coerceAtLeast(1),
                weightKg = weightKg.coerceAtLeast(0.1),
                avatarEmoji = emoji
            )
            val newId = repository.insertCat(newCat)
            _selectedCatId.value = newId.toInt()
            resetAdvice()
        }
    }

    // Delete a cat
    fun deleteCat(cat: Cat) {
        viewModelScope.launch {
            repository.deleteCat(cat)
            if (_selectedCatId.value == cat.id) {
                _selectedCatId.value = null
            }
            resetAdvice()
        }
    }

    // Log a new metric
    fun logMetric(type: String, metricName: String, valueDouble: Double, valueString: String = "") {
        val catId = selectedCatId.value ?: return
        viewModelScope.launch {
            val log = TrackingLog(
                catId = catId,
                dateString = _selectedDate.value,
                type = type,
                metricName = metricName,
                valueDouble = valueDouble,
                valueString = valueString
            )
            repository.insertLog(log)
            resetAdvice() // Reset insights so they reflect the new log
        }
    }

    // Delete a log record
    fun deleteLog(log: TrackingLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
            resetAdvice()
        }
    }

    // Delete log by ID
    fun deleteLogById(id: Int) {
        viewModelScope.launch {
            repository.deleteLogById(id)
            resetAdvice()
        }
    }

    // Select a cat profile
    fun selectCat(catId: Int) {
        _selectedCatId.value = catId
        resetAdvice()
    }

    // Add a reminder
    fun addReminder(title: String, type: String, dueDateString: String, notes: String = "") {
        val catId = selectedCatId.value ?: return
        viewModelScope.launch {
            val reminder = Reminder(
                catId = catId,
                title = title,
                type = type,
                dueDateString = dueDateString,
                notes = notes,
                isCompleted = false
            )
            repository.insertReminder(reminder)
        }
    }

    // Toggle reminder status
    fun toggleReminderCompleted(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    // Delete a reminder
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // Change tracking date (e.g. go back in time or forward)
    fun changeDate(offsetDays: Long) {
        try {
            val current = LocalDate.parse(_selectedDate.value)
            val newDate = current.plusDays(offsetDays)
            // Limit to today or past
            if (!newDate.isAfter(LocalDate.now())) {
                _selectedDate.value = newDate.toString()
                resetAdvice()
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    // Reset AI advice status
    fun resetAdvice() {
        _aiAdviceState.value = AiAdviceState.Idle
    }

    // Generate smart advice based on logged data
    fun generateAiAdvice() {
        val cat = selectedCat.value ?: return
        val logs = dailyLogs.value

        _aiAdviceState.value = AiAdviceState.Loading

        viewModelScope.launch {
            // Check if API key is missing
            if (!GeminiService.isApiKeyConfigured()) {
                _aiAdviceState.value = AiAdviceState.MissingKey
                return@launch
            }

            // Construct prompt based on logs
            val prompt = buildPrompt(cat, logs)

            val advice = GeminiService.getCatAdvice(prompt)
            if (advice.startsWith("API_ERROR:")) {
                _aiAdviceState.value = AiAdviceState.Error(advice.removePrefix("API_ERROR: "))
            } else {
                _aiAdviceState.value = AiAdviceState.Success(advice)
            }
        }
    }

    private fun buildPrompt(cat: Cat, logs: List<TrackingLog>): String {
        val ageYears = cat.ageMonths / 12
        val ageRemainingMonths = cat.ageMonths % 12
        val ageStr = buildString {
            if (ageYears > 0) append("$ageYears year(s) ")
            if (ageRemainingMonths > 0 || ageYears == 0) append("$ageRemainingMonths month(s)")
        }.trim()

        val logsSummary = if (logs.isEmpty()) {
            "No data has been logged yet for today."
        } else {
            logs.joinToString("\n") { log ->
                val metricLabel = when (log.metricName) {
                    "dry_food" -> "Dry Food"
                    "wet_food" -> "Wet Food"
                    "water" -> "Water Intake"
                    "playtime" -> "Playtime"
                    "sleep" -> "Sleep Duration"
                    "weight" -> "Weight Record"
                    "temp" -> "Body Temperature"
                    "mood" -> "Mood/Vibe"
                    "notes" -> "Symptom Notes"
                    else -> log.metricName.replace("_", " ").capitalize()
                }
                val valueUnit = when (log.metricName) {
                    "dry_food", "wet_food" -> "${log.valueDouble}g"
                    "water" -> "${log.valueDouble}ml"
                    "playtime" -> "${log.valueDouble} mins"
                    "sleep" -> "${log.valueDouble} hours"
                    "weight" -> "${log.valueDouble}kg"
                    "temp" -> "${log.valueDouble}°C"
                    else -> ""
                }
                val details = if (log.valueString.isNotEmpty()) " (${log.valueString})" else ""
                "- $metricLabel: $valueUnit$details"
            }
        }

        return """
            You are a professional, friendly, and expert cat veterinarian and behaviorist.
            I want advice for my cat companion:
            - Name: ${cat.name}
            - Breed: ${cat.breed}
            - Age: $ageStr
            - Current Weight: ${cat.weightKg} kg

            Today's logged metrics:
            $logsSummary

            Based on this information:
            1. Analyze today's food, water, and activity logs. Are they meeting standard healthy requirements?
            2. Offer 1-2 clear, actionable recommendations tailored to ${cat.name}'s breed/age/weight and today's logs to support their diet, hydration, or activity level.
            
            Keep your response highly informative, engaging, and direct. Format beautifully in Markdown using emojis and friendly bullet points. Keep it brief (around 120-150 words).
        """.trimIndent()
    }

    // Helper extension
    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    companion object {
        fun provideFactory(repository: CatRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CatTrackerViewModel(repository) as T
                }
            }
    }
}
