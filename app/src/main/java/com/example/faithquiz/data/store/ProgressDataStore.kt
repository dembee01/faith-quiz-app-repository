package com.example.faithquiz.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "faith_quiz_progress")

object ProgressDataStore {
	private val KEY_HIGHEST_UNLOCKED = intPreferencesKey("highest_unlocked_level")
	private val KEY_TOTAL_ANSWERED = intPreferencesKey("total_questions_answered")
	private val KEY_HIGH_SCORE = intPreferencesKey("high_score")
	private val KEY_LAST_COMPLETED = intPreferencesKey("last_completed_level")
	private val KEY_TOTAL_ATTEMPTS = intPreferencesKey("total_attempts")

	// Streaks and achievements
	private val KEY_DAILY_STREAK = intPreferencesKey("daily_streak")
	private val KEY_LAST_QUIZ_DAY = longPreferencesKey("last_quiz_epoch_day")
	private val KEY_ACHIEVEMENT_PERFECT = booleanPreferencesKey("achievement_perfect_score")
	private val KEY_ACHIEVEMENT_STREAK7 = booleanPreferencesKey("achievement_streak_7")

	// Adaptive difficulty recommended level
	private val KEY_ADAPTIVE_LEVEL = intPreferencesKey("adaptive_level")

	// Review lists (CSV of keys like "level|question")
	private val KEY_MISTAKE_KEYS = stringPreferencesKey("mistake_keys")
	private val KEY_BOOKMARK_KEYS = stringPreferencesKey("bookmark_keys")

	// Daily Devotion streak tracking
	private val KEY_DEVOTION_STREAK = intPreferencesKey("devotion_streak")
	private val KEY_LAST_DEVOTION_DAY = longPreferencesKey("last_devotion_epoch_day")
	
	// Topic Quiz scores
	private val KEY_GOSPELS_SCORE = intPreferencesKey("gospels_score")
	private val KEY_PROPHETS_SCORE = intPreferencesKey("prophets_score")
	private val KEY_PARABLES_SCORE = intPreferencesKey("parables_score")

	// App appearance settings
	private val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "light" or "dark"

	// Topic session persistence (resume current question and score)
	private val KEY_TOPIC_CURRENT_Q = intPreferencesKey("topic_current_q")
	private val KEY_TOPIC_SCORE = intPreferencesKey("topic_current_score")
	private val KEY_TOPIC_ID = stringPreferencesKey("topic_current_id")

	// SRS scheduling keys
	private val KEY_SRS_DUE = stringPreferencesKey("srs_due_map") // key->epochDay CSV entries key:day

	// Time tracking and last attempt summary
	private val KEY_TOTAL_TIME_SPENT = longPreferencesKey("total_time_spent_seconds")
	private val KEY_LAST_ATTEMPT_LEVEL = intPreferencesKey("last_attempt_level")
	private val KEY_LAST_ATTEMPT_SCORE = intPreferencesKey("last_attempt_score")
	private val KEY_LAST_ATTEMPT_TOTAL = intPreferencesKey("last_attempt_total")
	private val KEY_LAST_ATTEMPT_TIME = intPreferencesKey("last_attempt_time_seconds")
	private val KEY_LAST_ATTEMPT_DATE = longPreferencesKey("last_attempt_date")
	private val KEY_LAST_ATTEMPT_MODE = stringPreferencesKey("last_attempt_mode")

	// Detailed mistakes log (ordered CSV of encoded entries)
	private val KEY_MISTAKE_LOG = stringPreferencesKey("mistake_log")

	// --- Quiz Session Persistence (Resume Functionality) ---
	private val KEY_SESSION_LEVEL = intPreferencesKey("session_level")
	private val KEY_SESSION_MODE = stringPreferencesKey("session_mode")
	private val KEY_SESSION_INDEX = intPreferencesKey("session_index")
	private val KEY_SESSION_SCORE = intPreferencesKey("session_score")
	private val KEY_SESSION_TIME = longPreferencesKey("session_time_elapsed")
	private val KEY_SESSION_SEED = longPreferencesKey("session_seed")
	private val KEY_SESSION_LIVES = intPreferencesKey("session_lives")

	data class QuizSession(
		val level: Int,
		val mode: String,
		val index: Int,
		val score: Int,
		val time: Long, // Total elapsed seconds
		val seed: Long,
		val lives: Int
	)

	fun observeQuizSession(context: Context): Flow<QuizSession?> {
		return context.progressDataStore.data.map { prefs ->
			val level = prefs[KEY_SESSION_LEVEL] ?: -1
			if (level == -1) {
				null
			} else {
				QuizSession(
					level = level,
					mode = prefs[KEY_SESSION_MODE] ?: "classic",
					index = prefs[KEY_SESSION_INDEX] ?: 0,
					score = prefs[KEY_SESSION_SCORE] ?: 0,
					time = prefs[KEY_SESSION_TIME] ?: 0L,
					seed = prefs[KEY_SESSION_SEED] ?: 0L,
					lives = prefs[KEY_SESSION_LIVES] ?: 0
				)
			}
		}
	}

	suspend fun saveQuizSession(
		context: Context,
		session: QuizSession
	) {
		context.progressDataStore.edit { prefs ->
			prefs[KEY_SESSION_LEVEL] = session.level
			prefs[KEY_SESSION_MODE] = session.mode
			prefs[KEY_SESSION_INDEX] = session.index
			prefs[KEY_SESSION_SCORE] = session.score
			prefs[KEY_SESSION_TIME] = session.time
			prefs[KEY_SESSION_SEED] = session.seed
			prefs[KEY_SESSION_LIVES] = session.lives
		}
	}

	suspend fun clearQuizSession(context: Context) {
		context.progressDataStore.edit { prefs ->
			prefs.remove(KEY_SESSION_LEVEL)
			prefs.remove(KEY_SESSION_MODE)
			prefs.remove(KEY_SESSION_INDEX)
			prefs.remove(KEY_SESSION_SCORE)
			prefs.remove(KEY_SESSION_TIME)
			prefs.remove(KEY_SESSION_SEED)
			prefs.remove(KEY_SESSION_LIVES)
		}
	}
	// ----------------------------------------------------

	fun observeHighestUnlockedLevel(context: Context): Flow<Int> {
		return context.progressDataStore.data.map { prefs ->
			prefs[KEY_HIGHEST_UNLOCKED] ?: 1
		}
	}

	fun observeTotalQuestionsAnswered(context: Context): Flow<Int> {
		return context.progressDataStore.data.map { prefs ->
			prefs[KEY_TOTAL_ANSWERED] ?: 0
		}
	}

	fun observeHighScore(context: Context): Flow<Int> {
		return context.progressDataStore.data.map { prefs ->
			prefs[KEY_HIGH_SCORE] ?: 0
		}
	}

	fun observeLastCompletedLevel(context: Context): Flow<Int> {
		return context.progressDataStore.data.map { prefs ->
			prefs[KEY_LAST_COMPLETED] ?: 1
		}
	}

	suspend fun setHighestUnlockedLevel(context: Context, level: Int) {
		context.progressDataStore.edit { prefs ->
			val current = prefs[KEY_HIGHEST_UNLOCKED] ?: 1
			if (level > current) {
				prefs[KEY_HIGHEST_UNLOCKED] = level.coerceIn(1, 30)
			}
		}
	}

	suspend fun unlockNextLevel(context: Context, currentLevel: Int) {
		val nextLevel = (currentLevel + 1).coerceAtMost(30)
		setHighestUnlockedLevel(context, nextLevel)
	}

	suspend fun incrementQuestionsAnswered(context: Context, amount: Int = 1) {
		context.progressDataStore.edit { prefs ->
			val current = prefs[KEY_TOTAL_ANSWERED] ?: 0
			prefs[KEY_TOTAL_ANSWERED] = (current + amount).coerceAtLeast(0)
		}
	}

	suspend fun setHighScoreIfGreater(context: Context, newScore: Int) {
		context.progressDataStore.edit { prefs ->
			val current = prefs[KEY_HIGH_SCORE] ?: 0
			if (newScore > current) {
				prefs[KEY_HIGH_SCORE] = newScore
			}
		}
	}

	suspend fun setLastCompletedLevel(context: Context, level: Int) {
		context.progressDataStore.edit { prefs ->
			prefs[KEY_LAST_COMPLETED] = level.coerceIn(1, 30)
		}
	}

	/** Reset all stored progress to defaults */
	suspend fun reset(context: Context) {
		context.progressDataStore.edit { prefs ->
			prefs[KEY_HIGHEST_UNLOCKED] = 1
			prefs[KEY_TOTAL_ANSWERED] = 0
			prefs[KEY_HIGH_SCORE] = 0
			prefs[KEY_LAST_COMPLETED] = 1
			prefs[KEY_DAILY_STREAK] = 0
			prefs[KEY_LAST_QUIZ_DAY] = 0L
			prefs[KEY_ACHIEVEMENT_PERFECT] = false
			prefs[KEY_ACHIEVEMENT_STREAK7] = false
			prefs[KEY_ADAPTIVE_LEVEL] = 1
			prefs[KEY_MISTAKE_KEYS] = ""
			prefs[KEY_BOOKMARK_KEYS] = ""
			prefs[KEY_DEVOTION_STREAK] = 0
			prefs[KEY_LAST_DEVOTION_DAY] = 0L
			prefs[KEY_SRS_DUE] = "" // Clear SRS data on reset
			prefs[KEY_THEME_MODE] = "light"

			// New: attempts, time, last attempt summary, detailed mistakes
			prefs[KEY_TOTAL_ATTEMPTS] = 0
			prefs[KEY_TOTAL_TIME_SPENT] = 0L
			prefs[KEY_LAST_ATTEMPT_LEVEL] = 0
			prefs[KEY_LAST_ATTEMPT_SCORE] = 0
			prefs[KEY_LAST_ATTEMPT_TOTAL] = 0
			prefs[KEY_LAST_ATTEMPT_TIME] = 0
			prefs[KEY_LAST_ATTEMPT_DATE] = 0L
			prefs[KEY_LAST_ATTEMPT_MODE] = ""
			prefs[KEY_MISTAKE_LOG] = ""
			
			// Clear active session
			prefs.remove(KEY_SESSION_LEVEL)
			prefs.remove(KEY_SESSION_MODE)
			prefs.remove(KEY_SESSION_INDEX)
			prefs.remove(KEY_SESSION_SCORE)
			prefs.remove(KEY_SESSION_TIME)
			prefs.remove(KEY_SESSION_SEED)
			prefs.remove(KEY_SESSION_LIVES)
		}
	}

	// Observers
	fun observeDailyStreak(context: Context): Flow<Int> = context.progressDataStore.data.map { it[KEY_DAILY_STREAK] ?: 0 }
	fun observeAchievements(context: Context): Flow<Pair<Boolean, Boolean>> = context.progressDataStore.data.map {
		val perfect = it[KEY_ACHIEVEMENT_PERFECT] ?: false
		val streak7 = it[KEY_ACHIEVEMENT_STREAK7] ?: false
		perfect to streak7
	}
	fun observeAdaptiveLevel(context: Context): Flow<Int> = context.progressDataStore.data.map { it[KEY_ADAPTIVE_LEVEL] ?: 1 }
	fun observeBookmarks(context: Context): Flow<List<String>> = context.progressDataStore.data.map { (it[KEY_BOOKMARK_KEYS] ?: "").toKeyList() }
	fun observeMistakes(context: Context): Flow<List<String>> {
		return context.progressDataStore.data.map { prefs ->
			val log = prefs[KEY_MISTAKE_LOG]
			when {
				!log.isNullOrBlank() -> log.toKeyList()
				else -> (prefs[KEY_MISTAKE_KEYS] ?: "").toKeyList()
			}
		}
	}
	fun observeDevotionStreak(context: Context): Flow<Int> = context.progressDataStore.data.map { it[KEY_DEVOTION_STREAK] ?: 0 }

	// Update helpers and analytics
	fun observeTotalAttempts(context: Context): Flow<Int> = context.progressDataStore.data.map { it[KEY_TOTAL_ATTEMPTS] ?: 0 }
	suspend fun incrementTotalAttempts(context: Context) {
		context.progressDataStore.edit { prefs ->
			val current = prefs[KEY_TOTAL_ATTEMPTS] ?: 0
			prefs[KEY_TOTAL_ATTEMPTS] = (current + 1).coerceAtLeast(0)
		}
	}

	fun observeTotalTimeSpent(context: Context): Flow<Long> = context.progressDataStore.data.map { it[KEY_TOTAL_TIME_SPENT] ?: 0L }
	suspend fun addTimeSpentSeconds(context: Context, seconds: Int) {
		context.progressDataStore.edit { prefs ->
			val cur = prefs[KEY_TOTAL_TIME_SPENT] ?: 0L
			prefs[KEY_TOTAL_TIME_SPENT] = (cur + seconds.coerceAtLeast(0)).coerceAtLeast(0L)
		}
	}

	data class LastAttempt(
		val level: Int,
		val score: Int,
		val total: Int,
		val timeSeconds: Int,
		val date: Long,
		val mode: String
	)

	fun observeLastAttempt(context: Context): Flow<LastAttempt?> {
		return context.progressDataStore.data.map { prefs ->
			val total = prefs[KEY_LAST_ATTEMPT_TOTAL] ?: 0
			val score = prefs[KEY_LAST_ATTEMPT_SCORE] ?: 0
			val level = prefs[KEY_LAST_ATTEMPT_LEVEL] ?: 0
			val time = prefs[KEY_LAST_ATTEMPT_TIME] ?: 0
			val date = prefs[KEY_LAST_ATTEMPT_DATE] ?: 0L
			val mode = prefs[KEY_LAST_ATTEMPT_MODE] ?: ""
			if (total == 0 && score == 0 && level == 0 && time == 0 && date == 0L && mode.isEmpty()) null
			else LastAttempt(level, score, total, time, date, mode)
		}
	}

	suspend fun setLastAttemptSummary(
		context: Context,
		level: Int,
		score: Int,
		total: Int,
		timeSeconds: Int,
		mode: String
	) {
		context.progressDataStore.edit { prefs ->
			prefs[KEY_LAST_ATTEMPT_LEVEL] = level
			prefs[KEY_LAST_ATTEMPT_SCORE] = score
			prefs[KEY_LAST_ATTEMPT_TOTAL] = total
			prefs[KEY_LAST_ATTEMPT_TIME] = timeSeconds
			prefs[KEY_LAST_ATTEMPT_DATE] = System.currentTimeMillis()
			prefs[KEY_LAST_ATTEMPT_MODE] = mode
		}
	}

	// Detailed mistakes API (ordered log with details)
	data class MistakeEntry(
		val level: Int,
		val date: Long,
		val question: String,
		val userAnswer: String,
		val correctAnswer: String,
		val explanation: String
	)

	fun observeMistakesDetailed(context: Context): Flow<List<MistakeEntry>> {
		return context.progressDataStore.data.map { prefs ->
			val raw = prefs[KEY_MISTAKE_LOG] ?: ""
			raw.toKeyList().mapNotNull { decodeMistakeEntry(it) }
		}
	}

	suspend fun addMistakeDetailed(
		context: Context,
		level: Int,
		question: String,
		userAnswer: String,
		correctAnswer: String,
		explanation: String?
	) {
		context.progressDataStore.edit { prefs ->
			val now = System.currentTimeMillis()
			val newEntry = encodeMistakeEntry(
				MistakeEntry(
					level = level,
					date = now,
					question = question,
					userAnswer = userAnswer,
					correctAnswer = correctAnswer,
					explanation = explanation.orEmpty()
				)
			)
			val existing = (prefs[KEY_MISTAKE_LOG] ?: "")
			val list = existing.toKeyList().toMutableList()
			list.add(0, newEntry) // prepend newest
			// keep last 200
			val trimmed = list.take(200).joinToString(",")
			prefs[KEY_MISTAKE_LOG] = trimmed

			// Maintain legacy keys for compatibility
			val legacySet = (prefs[KEY_MISTAKE_KEYS] ?: "").toKeyList().toMutableSet()
			val legacyKey = "$level|$question"
			if (legacySet.add(legacyKey)) {
				prefs[KEY_MISTAKE_KEYS] = legacySet.joinToString(",")
			}
		}
	}

	private fun encode(str: String): String = URLEncoder.encode(str, StandardCharsets.UTF_8.toString())
	private fun decode(str: String): String = URLDecoder.decode(str, StandardCharsets.UTF_8.toString())

	private fun encodeMistakeEntry(entry: MistakeEntry): String {
		// Format: level|date|q|user|correct|explanation (each URL-encoded except numbers)
		return listOf(
			entry.level.toString(),
			entry.date.toString(),
			encode(entry.question),
			encode(entry.userAnswer),
			encode(entry.correctAnswer),
			encode(entry.explanation)
		).joinToString("|")
	}

	private fun decodeMistakeEntry(raw: String): MistakeEntry? {
		val parts = raw.split("|")
		return try {
			if (parts.size < 6) null else MistakeEntry(
				level = parts[0].toInt(),
				date = parts[1].toLong(),
				question = decode(parts[2]),
				userAnswer = decode(parts[3]),
				correctAnswer = decode(parts[4]),
				explanation = decode(parts[5])
			)
		} catch (_: Exception) {
			null
		}
	}

	// Update helpers
	suspend fun recordQuizCompletion(context: Context, score: Int, totalQuestions: Int) {
		context.progressDataStore.edit { prefs ->
			// High score already handled elsewhere if needed
			// Daily streak logic (based on epoch days)
			val millisPerDay = 86_400_000L
			val todayEpochDay = System.currentTimeMillis() / millisPerDay
			val lastDay = prefs[KEY_LAST_QUIZ_DAY] ?: -1L
			val currentStreak = prefs[KEY_DAILY_STREAK] ?: 0
			val newStreak = when {
				lastDay == todayEpochDay -> currentStreak // already counted today
				lastDay == todayEpochDay - 1 -> currentStreak + 1
				else -> 1
			}
			prefs[KEY_DAILY_STREAK] = newStreak
			prefs[KEY_LAST_QUIZ_DAY] = todayEpochDay

			// Achievements
			if (totalQuestions > 0 && score == totalQuestions) {
				prefs[KEY_ACHIEVEMENT_PERFECT] = true
			}
			if (newStreak >= 7) {
				prefs[KEY_ACHIEVEMENT_STREAK7] = true
			}
		}
	}

	suspend fun updateAdaptiveLevelFromAccuracy(context: Context, accuracyPercent: Int) {
		context.progressDataStore.edit { prefs ->
			val current = prefs[KEY_ADAPTIVE_LEVEL] ?: 1
			val updated = when {
				accuracyPercent >= 80 -> (current + 1).coerceAtMost(30)
				accuracyPercent <= 40 -> (current - 1).coerceAtLeast(1)
				else -> current
			}
			prefs[KEY_ADAPTIVE_LEVEL] = updated
		}
	}

	// Review lists: keys are simple strings like "<level>|<question>"
	suspend fun addMistakeKey(context: Context, key: String) {
		context.progressDataStore.edit { prefs ->
			val set = (prefs[KEY_MISTAKE_KEYS] ?: "").toKeyList().toMutableSet()
			if (set.add(key)) prefs[KEY_MISTAKE_KEYS] = set.joinToString(",")
			// Schedule SRS for tomorrow
			val due = parseDueMap(prefs[KEY_SRS_DUE])
			val tomorrow = epochDayNow() + 1
			due[key] = tomorrow
			prefs[KEY_SRS_DUE] = serializeDueMap(due)
		}
	}

	suspend fun addBookmarkKey(context: Context, key: String) {
		context.progressDataStore.edit { prefs ->
			val set = (prefs[KEY_BOOKMARK_KEYS] ?: "").toKeyList().toMutableSet()
			if (set.add(key)) prefs[KEY_BOOKMARK_KEYS] = set.joinToString(",")
			// Schedule SRS for tomorrow
			val due = parseDueMap(prefs[KEY_SRS_DUE])
			val tomorrow = epochDayNow() + 1
			due[key] = tomorrow
			prefs[KEY_SRS_DUE] = serializeDueMap(due)
		}
	}

	suspend fun removeBookmarkKey(context: Context, key: String) {
		context.progressDataStore.edit { prefs ->
			val list = (prefs[KEY_BOOKMARK_KEYS] ?: "").toKeyList().toMutableList()
			if (list.remove(key)) prefs[KEY_BOOKMARK_KEYS] = list.joinToString(",")
		}
	}

	fun isBookmarkedSync(prefs: Preferences, key: String): Boolean {
		return (prefs[KEY_BOOKMARK_KEYS] ?: "").toKeyList().contains(key)
	}

	// SRS API
	fun observeDueReview(context: Context): Flow<List<String>> = context.progressDataStore.data.map { prefs ->
		val today = epochDayNow()
		parseDueMap(prefs[KEY_SRS_DUE]).filter { (_, day) -> day <= today }.keys.toList()
	}

	suspend fun recordReviewResult(context: Context, key: String, wasCorrect: Boolean) {
		context.progressDataStore.edit { prefs ->
			val due = parseDueMap(prefs[KEY_SRS_DUE])
			val currentDue = due[key] ?: epochDayNow()
			val nextInterval = when {
				!wasCorrect -> 1 // repeat tomorrow on failure
				currentDue <= epochDayNow() + 1 -> 3
				currentDue <= epochDayNow() + 3 -> 7
				currentDue <= epochDayNow() + 7 -> 14
				else -> 30
			}
			due[key] = epochDayNow() + nextInterval
			prefs[KEY_SRS_DUE] = serializeDueMap(due)
		}
	}

	suspend fun removeFromSrs(context: Context, key: String) {
		context.progressDataStore.edit { prefs ->
			val due = parseDueMap(prefs[KEY_SRS_DUE])
			due.remove(key)
			prefs[KEY_SRS_DUE] = serializeDueMap(due)
		}
	}

	private fun epochDayNow(): Long = System.currentTimeMillis() / 86_400_000L
	private fun parseDueMap(raw: String?): MutableMap<String, Long> {
		if (raw.isNullOrBlank()) return mutableMapOf()
		return raw.split(',').mapNotNull { entry ->
			val parts = entry.split(':')
			if (parts.size == 2) parts[0] to (parts[1].toLongOrNull() ?: return@mapNotNull null) else null
		}.toMap(mutableMapOf())
	}
	private fun serializeDueMap(map: Map<String, Long>): String = map.entries.joinToString(",") { it.key + ":" + it.value }

	// Daily Devotion completion handling
	suspend fun recordDevotionCompletion(context: Context) {
		context.progressDataStore.edit { prefs ->
			val millisPerDay = 86_400_000L
			val todayEpochDay = System.currentTimeMillis() / millisPerDay
			val last = prefs[KEY_LAST_DEVOTION_DAY] ?: -1L
			val current = prefs[KEY_DEVOTION_STREAK] ?: 0
			val updated = when {
				last == todayEpochDay -> current
				last == todayEpochDay - 1 -> current + 1
				else -> 1
			}
			prefs[KEY_LAST_DEVOTION_DAY] = todayEpochDay
			prefs[KEY_DEVOTION_STREAK] = updated
		}
	}
	
	// Topic Quiz score functions
	fun observeTopicScore(context: Context, topic: String): Flow<Int> {
		return context.progressDataStore.data.map { prefs ->
			when (topic) {
				"gospels" -> prefs[KEY_GOSPELS_SCORE] ?: 0
				"prophets" -> prefs[KEY_PROPHETS_SCORE] ?: 0
				"parables" -> prefs[KEY_PARABLES_SCORE] ?: 0
				else -> 0
			}
		}
	}

	// Appearance settings
	fun observeThemeMode(context: Context): Flow<String> {
		return context.progressDataStore.data.map { prefs ->
			prefs[KEY_THEME_MODE] ?: "light"
		}
	}

	suspend fun setThemeMode(context: Context, mode: String) {
		context.progressDataStore.edit { prefs ->
			prefs[KEY_THEME_MODE] = if (mode == "dark") "dark" else "light"
		}
	}

	// --- Topic session state ---
	fun observeTopicSession(context: Context): Flow<Triple<String, Int, Int>> {
		return context.progressDataStore.data.map { prefs ->
			Triple(
				prefs[KEY_TOPIC_ID] ?: "",
				prefs[KEY_TOPIC_CURRENT_Q] ?: 0,
				prefs[KEY_TOPIC_SCORE] ?: 0
			)
		}
	}

	suspend fun saveTopicSession(context: Context, topic: String, currentIndex: Int, score: Int) {
		context.progressDataStore.edit { prefs ->
			prefs[KEY_TOPIC_ID] = topic
			prefs[KEY_TOPIC_CURRENT_Q] = currentIndex
			prefs[KEY_TOPIC_SCORE] = score
		}
	}

	suspend fun clearTopicSession(context: Context) {
		context.progressDataStore.edit { prefs ->
			prefs[KEY_TOPIC_ID] = ""
			prefs[KEY_TOPIC_CURRENT_Q] = 0
			prefs[KEY_TOPIC_SCORE] = 0
		}
	}
	
	suspend fun setTopicScore(context: Context, topic: String, score: Int) {
		context.progressDataStore.edit { prefs ->
			when (topic) {
				"gospels" -> prefs[KEY_GOSPELS_SCORE] = score.coerceIn(0, 50)
				"prophets" -> prefs[KEY_PROPHETS_SCORE] = score.coerceIn(0, 50)
				"parables" -> prefs[KEY_PARABLES_SCORE] = score.coerceIn(0, 50)
			}
		}
	}
	
	suspend fun updateTopicScoreIfHigher(context: Context, topic: String, newScore: Int) {
		context.progressDataStore.edit { prefs ->
			val currentScore = when (topic) {
				"gospels" -> prefs[KEY_GOSPELS_SCORE] ?: 0
				"prophets" -> prefs[KEY_PROPHETS_SCORE] ?: 0
				"parables" -> prefs[KEY_PARABLES_SCORE] ?: 0
				else -> 0
			}
			
			if (newScore > currentScore) {
				when (topic) {
					"gospels" -> prefs[KEY_GOSPELS_SCORE] = newScore.coerceIn(0, 50)
					"prophets" -> prefs[KEY_PROPHETS_SCORE] = newScore.coerceIn(0, 50)
					"parables" -> prefs[KEY_PARABLES_SCORE] = newScore.coerceIn(0, 50)
				}
			}
		}
	}
}

private fun String.toKeyList(): List<String> {
	return this.split(',').mapNotNull { part ->
		val p = part.trim()
		if (p.isEmpty()) null else p
	}
}
