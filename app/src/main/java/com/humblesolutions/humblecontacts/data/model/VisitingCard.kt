package com.humblesolutions.humblecontacts.data.model

import androidx.annotation.Keep

@Keep
data class VisitingCard(
    val cardTheme: String = "",
    val bio: String = ""
)