package com.example.data

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesUtil {
    private const val PREFS_NAME = "scratch_teaching_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ROLE = "role" // "student" or "teacher"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_CLASS_ID = "class_id"
    private const val KEY_IDENTIFIER = "identifier" // 学号或者工号

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLoginSession(
        context: Context,
        userId: Int,
        role: String,
        userName: String,
        classId: Int = 0,
        identifier: String
    ) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, userId)
            putString(KEY_ROLE, role)
            putString(KEY_USER_NAME, userName)
            putInt(KEY_CLASS_ID, classId)
            putString(KEY_IDENTIFIER, identifier)
            apply()
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(context: Context): Int {
        return getPrefs(context).getInt(KEY_USER_ID, -1)
    }

    fun getRole(context: Context): String? {
        return getPrefs(context).getString(KEY_ROLE, null)
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "") ?: ""
    }

    fun getClassId(context: Context): Int {
        return getPrefs(context).getInt(KEY_CLASS_ID, 0)
    }

    fun getIdentifier(context: Context): String {
        return getPrefs(context).getString(KEY_IDENTIFIER, "") ?: ""
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun saveClassDescription(context: Context, classId: Int, description: String) {
        getPrefs(context).edit().putString("class_desc_$classId", description).apply()
    }

    fun getClassDescription(context: Context, classId: Int): String {
        return getPrefs(context).getString("class_desc_$classId", "暂无描述") ?: "暂无描述"
    }
}
