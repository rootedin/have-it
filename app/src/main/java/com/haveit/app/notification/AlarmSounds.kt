package com.haveit.app.notification

import android.content.Context
import android.net.Uri
import com.haveit.app.R

/**
 * The app's own bundled alarm tones (res/raw), so reminders don't depend on whatever the system
 * default alarm happens to be. Each [key] is stored in settings; [resId] points at the raw clip.
 */
object AlarmSounds {

    data class AlarmSound(val key: String, val label: String, val resId: Int)

    val ALL: List<AlarmSound> = listOf(
        AlarmSound("pulse", "펄스", R.raw.alarm_pulse),
        AlarmSound("beepbeep", "삐삐", R.raw.alarm_beepbeep),
        AlarmSound("digital", "디지털", R.raw.alarm_digital),
        AlarmSound("bell", "종소리", R.raw.alarm_bell),
        AlarmSound("chime", "차임벨", R.raw.alarm_chime),
        AlarmSound("marimba", "마림바", R.raw.alarm_marimba),
        AlarmSound("arpeggio", "아르페지오", R.raw.alarm_arpeggio),
        AlarmSound("crystal", "크리스탈", R.raw.alarm_crystal),
        AlarmSound("sunrise", "선라이즈", R.raw.alarm_sunrise),
        AlarmSound("warm", "웜", R.raw.alarm_warm),
    )

    // Keep in sync with UserSettingsRepository.DEFAULT_ALARM_SOUND_KEY.
    const val DEFAULT_KEY = "pulse"

    fun byKey(key: String?): AlarmSound =
        ALL.firstOrNull { it.key == key } ?: ALL.first { it.key == DEFAULT_KEY }

    fun uri(context: Context, key: String?): Uri =
        Uri.parse("android.resource://${context.packageName}/${byKey(key).resId}")
}
