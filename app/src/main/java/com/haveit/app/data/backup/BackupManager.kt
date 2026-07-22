package com.haveit.app.data.backup

import android.content.Context
import android.net.Uri
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.CheckInEntity
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.data.local.entity.HabitFrequency
import com.haveit.app.data.local.entity.RoutineEntity
import com.haveit.app.data.settings.AppTheme
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only JSON backup. There is no server, so this is the user's path for moving
 * data to a new device or recovering after a reinstall. Import replaces all existing data.
 */
class BackupManager(private val app: HaveItApplication) {

    suspend fun exportToJson(): String {
        val habits = app.container.habitRepository.getAll()
        val checkIns = app.container.checkInRepository.getAll()
        val routines = app.container.routineRepository.getAll()
        val settings = app.container.userSettingsRepository.settings.first()

        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("habits", JSONArray().apply {
            habits.forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id)
                    put("name", h.name)
                    put("icon", h.icon)
                    put("color", h.color)
                    put("frequency", h.frequency.name)
                    put("customDays", h.customDays?.let { JSONArray(it) } ?: JSONArray())
                    put("reminderHour", h.reminderHour ?: JSONObject.NULL)
                    put("reminderMinute", h.reminderMinute ?: JSONObject.NULL)
                    put("reminderSnoozeMinutes", h.reminderSnoozeMinutes)
                    put("reminderSnoozeMaxCount", h.reminderSnoozeMaxCount)
                    put("createdAt", h.createdAt)
                    put("archivedAt", h.archivedAt ?: JSONObject.NULL)
                })
            }
        })

        root.put("checkIns", JSONArray().apply {
            checkIns.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("habitId", c.habitId)
                    put("epochDay", c.epochDay)
                    put("completed", c.completed)
                })
            }
        })

        root.put("routines", JSONArray().apply {
            routines.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("sortOrder", r.sortOrder)
                    put("orderedHabitIds", JSONArray(r.orderedHabitIds))
                })
            }
        })

        root.put("settings", JSONObject().apply {
            put("notificationsEnabled", settings.notificationsEnabled)
            put("theme", settings.theme.name)
            put("alarmSoundUri", settings.alarmSoundUri ?: JSONObject.NULL)
        })

        return root.toString(2)
    }

    suspend fun writeBackup(context: Context, uri: Uri) {
        val json = exportToJson()
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("백업 파일을 열 수 없어요")
    }

    suspend fun readBackup(context: Context, uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("백업 파일을 읽을 수 없어요")
        importFromJson(text)
    }

    suspend fun importFromJson(text: String) {
        val root = JSONObject(text)

        val habits = root.optJSONArray("habits").toObjectList().map { o ->
            val days = o.optJSONArray("customDays")
            HabitEntity(
                id = o.getString("id"),
                name = o.getString("name"),
                icon = o.optString("icon", "✅"),
                color = o.optString("color", "#7C6FF2"),
                frequency = HabitFrequency.valueOf(o.getString("frequency")),
                customDays = if (days != null && days.length() > 0) {
                    (0 until days.length()).map { days.getInt(it) }
                } else null,
                reminderHour = o.optIntOrNull("reminderHour"),
                reminderMinute = o.optIntOrNull("reminderMinute"),
                reminderSnoozeMinutes = o.optInt("reminderSnoozeMinutes", HabitEntity.DEFAULT_SNOOZE_MINUTES),
                reminderSnoozeMaxCount = o.optInt("reminderSnoozeMaxCount", HabitEntity.DEFAULT_SNOOZE_MAX_COUNT),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                archivedAt = o.optLongOrNull("archivedAt"),
            )
        }

        val checkIns = root.optJSONArray("checkIns").toObjectList().map { o ->
            CheckInEntity(
                id = o.getString("id"),
                habitId = o.getString("habitId"),
                epochDay = o.getLong("epochDay"),
                completed = o.optBoolean("completed", false),
            )
        }

        val routines = root.optJSONArray("routines").toObjectList().mapIndexed { index, o ->
            val ids = o.optJSONArray("orderedHabitIds")
            RoutineEntity(
                id = o.getString("id"),
                name = o.getString("name"),
                sortOrder = o.optInt("sortOrder", index),
                orderedHabitIds = if (ids != null) {
                    (0 until ids.length()).map { ids.getString(it) }
                } else emptyList(),
            )
        }

        // Replace-all restore.
        app.container.checkInRepository.clear()
        app.container.routineRepository.clear()
        app.container.habitRepository.clear()
        habits.forEach { app.container.habitRepository.upsert(it) }
        app.container.checkInRepository.upsertAll(checkIns)
        routines.forEach { app.container.routineRepository.upsert(it) }

        root.optJSONObject("settings")?.let { s ->
            app.container.userSettingsRepository.setNotificationsEnabled(
                s.optBoolean("notificationsEnabled", true),
            )
            app.container.userSettingsRepository.setAlarmSoundUri(
                s.optStringOrNull("alarmSoundUri"),
            )
            runCatching { AppTheme.valueOf(s.optString("theme", AppTheme.SYSTEM.name)) }
                .getOrNull()?.let { app.container.userSettingsRepository.setTheme(it) }
        }
    }

    companion object {
        const val BACKUP_VERSION = 1
        const val DEFAULT_FILENAME = "have-it-backup.json"

        private fun JSONArray?.toObjectList(): List<JSONObject> =
            if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

        private fun JSONObject.optStringOrNull(key: String): String? =
            if (isNull(key) || !has(key)) null else optString(key).ifBlank { null }

        private fun JSONObject.optIntOrNull(key: String): Int? =
            if (isNull(key) || !has(key)) null else optInt(key)

        private fun JSONObject.optLongOrNull(key: String): Long? =
            if (isNull(key) || !has(key)) null else optLong(key)
    }
}
