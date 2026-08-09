package com.example.faithquiz.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "power_ups")
data class PowerUp(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: PowerUpType,
    val count: Int = 0
)

enum class PowerUpType(val displayName: String, val description: String) {
    FIFTY_FIFTY("50/50", "Remove two incorrect answers"),
    SKIP_QUESTION("Skip", "Skip the current question"),
    EXTRA_TIME("Extra Time", "Add 30 seconds to the timer"),
    HINT("Hint", "Get a helpful hint for the question")
}
