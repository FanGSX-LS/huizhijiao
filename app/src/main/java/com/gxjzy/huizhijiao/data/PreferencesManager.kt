package com.gxjzy.huizhijiao.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gxjzy.huizhijiao.model.TemplateItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_ROLE = stringPreferencesKey("role")
        val KEY_LOGIN_NAME = stringPreferencesKey("login_name")
        val KEY_PASSWORD = stringPreferencesKey("password")
        val KEY_SCHOOL_ID = stringPreferencesKey("school_id")
        val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        val KEY_BACKGROUND_IMAGE = stringPreferencesKey("background_image")
        val KEY_ADVANCED_MODE = booleanPreferencesKey("advanced_mode")
        val KEY_ADVANCED_MODE_REVEALED = booleanPreferencesKey("advanced_mode_revealed")
        val KEY_LOGIN_UPLOADED = booleanPreferencesKey("login_uploaded")

    }

    val token: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val role: Flow<String?> = context.dataStore.data.map { it[KEY_ROLE] }
    val loginName: Flow<String?> = context.dataStore.data.map { it[KEY_LOGIN_NAME] }
    val password: Flow<String?> = context.dataStore.data.map { it[KEY_PASSWORD] }
    val schoolId: Flow<String?> = context.dataStore.data.map { it[KEY_SCHOOL_ID] }
    val apiBaseUrl: Flow<String?> = context.dataStore.data.map { it[KEY_API_BASE_URL] }
    val backgroundImage: Flow<String?> = context.dataStore.data.map { it[KEY_BACKGROUND_IMAGE] }
    val advancedMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_ADVANCED_MODE] ?: false }
    val advancedModeRevealed: Flow<Boolean> = context.dataStore.data.map { it[KEY_ADVANCED_MODE_REVEALED] ?: false }
    val loginUploaded: Flow<Boolean> = context.dataStore.data.map { it[KEY_LOGIN_UPLOADED] ?: false }


    suspend fun saveToken(value: String?) = setPreference(KEY_TOKEN, value)
    suspend fun saveRole(value: String?) = setPreference(KEY_ROLE, value)
    suspend fun saveLoginName(value: String?) = setPreference(KEY_LOGIN_NAME, value)
    suspend fun savePassword(value: String?) = setPreference(KEY_PASSWORD, value)
    suspend fun saveSchoolId(value: String?) = setPreference(KEY_SCHOOL_ID, value)
    suspend fun saveApiBaseUrl(value: String?) = setPreference(KEY_API_BASE_URL, value)
    suspend fun saveBackgroundImage(value: String?) = setPreference(KEY_BACKGROUND_IMAGE, value)
    suspend fun saveAdvancedMode(value: Boolean) = setPreference(KEY_ADVANCED_MODE, value)
    suspend fun saveAdvancedModeRevealed(value: Boolean) = setPreference(KEY_ADVANCED_MODE_REVEALED, value)
    suspend fun saveLoginUploaded(value: Boolean) = setPreference(KEY_LOGIN_UPLOADED, value)


    suspend fun clearCredentials() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_ROLE)
            prefs.remove(KEY_LOGIN_NAME)
            prefs.remove(KEY_PASSWORD)
            prefs.remove(KEY_SCHOOL_ID)
        }
    }

    private val gson = Gson()
    private val KEY_TEMPLATES = stringPreferencesKey("templates_json")

    suspend fun loadTemplates(): List<TemplateItem> {
        val json: String? = context.dataStore.data.map { it[KEY_TEMPLATES] }.first() as? String
        return if (json != null) {
            try { gson.fromJson(json, object : TypeToken<List<TemplateItem>>() {}.type) } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    suspend fun saveTemplates(templates: List<TemplateItem>) {
        val json = gson.toJson(templates)
        setPreference(KEY_TEMPLATES, json)
    }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T?) {
        context.dataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(key)
            } else {
                @Suppress("UNCHECKED_CAST")
                prefs[key] = value
            }
        }
    }
}
