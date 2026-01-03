package com.nigdroid.focusflow_nitr.data.model

import com.google.firebase.Timestamp

data class Reflection(
    val reflectionId: String = "",
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val mood: String = "",
    val textNote: String = "",
    val voiceNoteUrl: String = ""
)