package com.gxjzy.huizhijiao.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.data.AppDataCache
import com.gxjzy.huizhijiao.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import java.util.Calendar

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    LaunchedEffect(Unit) {
        val prefs = BRApp.instance.prefs
        val savedLoginName = prefs.loginName.first()
        val savedPassword = prefs.password.first()
        val savedSchoolId = prefs.schoolId.first()

        if (!savedLoginName.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            if (!savedSchoolId.isNullOrEmpty()) {
                val result = ApiClient.login(savedLoginName, savedPassword, savedSchoolId)
                if (result.success) {
                    ApiClient.setToken(result.token)
                    ApiClient.setCachedUser(result.user)
                    prefs.saveToken(result.token)
                    prefs.saveRole(result.role)
                    if (result.user?.advancedModeExpired == true) {
                        prefs.saveAdvancedMode(false)
                    }
                    preloadData()
                    onNavigateToMain()
                } else {
                    onNavigateToLogin()
                }
            } else {
                val result = ApiClient.adminLogin(savedLoginName, savedPassword)
                if (result.success) {
                    ApiClient.setToken(result.token)
                    prefs.saveToken(result.token)
                    prefs.saveRole(result.role)
                    onNavigateToAdmin()
                } else {
                    onNavigateToLogin()
                }
            }
        } else {
            onNavigateToLogin()
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().background(appBackground()),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

private suspend fun preloadData() {
    try {
        coroutineScope {
            val now = Calendar.getInstance()
            val year = now.get(Calendar.YEAR)
            val month = now.get(Calendar.MONTH) + 1
            AppDataCache.calendarYear = year
            AppDataCache.calendarMonth = month

            val userDeferred = async { ApiClient.fetchMe() }
            val progressDeferred = async { ApiClient.fetchCheckinProgress() }
            val calendarDeferred = async { ApiClient.fetchCheckinCalendar(year, month) }
            val writeRecordsDeferred = async { ApiClient.fetchWriteRecords() }
            val weeksDeferred = async { ApiClient.fetchWeeks() }
            val monthsDeferred = async { ApiClient.fetchMonths() }
            val weekListDeferred = async { ApiClient.fetchWeekList() }
            val monthListDeferred = async { ApiClient.fetchMonthList() }
            val weekTplDeferred = async { ApiClient.getTemplates("week") }
            val monthTplDeferred = async { ApiClient.getTemplates("month") }

            AppDataCache.userProfile = userDeferred.await()
            AppDataCache.checkinProgress = progressDeferred.await()
            AppDataCache.calendarEntries = calendarDeferred.await()
            AppDataCache.writeRecords = writeRecordsDeferred.await()
            AppDataCache.weekItems = weeksDeferred.await()
            AppDataCache.monthItems = monthsDeferred.await()
            AppDataCache.weekManageItems = weekListDeferred.await()
            AppDataCache.monthManageItems = monthListDeferred.await()
            AppDataCache.weekTemplates = weekTplDeferred.await()
            AppDataCache.monthTemplates = monthTplDeferred.await()

            val startDate = String.format("%d-%02d-01", year, month)
            val endDate = String.format("%d-%02d-%02d", year, month, getDaysInMonth(year, month))
            AppDataCache.checkinRecords = ApiClient.fetchCheckinRecords(startDate, endDate)

            val user = AppDataCache.userProfile
            val internshipId = user?.internship?.internshipId?.toString() ?: ""
            if (internshipId.isNotEmpty()) {
                AppDataCache.summaryItem = ApiClient.fetchSummary(internshipId)
            }
        }
    } catch (_: Exception) {}
}

private fun getDaysInMonth(year: Int, month: Int): Int = Calendar.getInstance().apply { set(Calendar.YEAR, year); set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
