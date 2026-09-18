package com.gxjzy.huizhijiao.data

import com.gxjzy.huizhijiao.model.UserInfo
import com.gxjzy.huizhijiao.model.CheckinProgressData
import com.gxjzy.huizhijiao.model.CalendarEntry
import com.gxjzy.huizhijiao.model.CheckinRecord
import com.gxjzy.huizhijiao.model.WriteRecordsResult
import com.gxjzy.huizhijiao.model.WeekItem
import com.gxjzy.huizhijiao.model.MonthItem
import com.gxjzy.huizhijiao.model.ManageItem
import com.gxjzy.huizhijiao.model.SummaryItem
import com.gxjzy.huizhijiao.model.TemplateItem

object AppDataCache {
    var userProfile: UserInfo? = null
    var checkinProgress: CheckinProgressData? = null
    var calendarEntries: List<CalendarEntry>? = null
    var checkinRecords: List<CheckinRecord>? = null
    var writeRecords: WriteRecordsResult? = null
    var weekItems: List<WeekItem>? = null
    var monthItems: List<MonthItem>? = null
    var weekManageItems: List<ManageItem>? = null
    var monthManageItems: List<ManageItem>? = null
    var summaryItem: SummaryItem? = null
    var weekTemplates: List<TemplateItem>? = null
    var monthTemplates: List<TemplateItem>? = null
    var calendarYear: Int = 0
    var calendarMonth: Int = 0

    fun clear() {
        userProfile = null
        checkinProgress = null
        calendarEntries = null
        checkinRecords = null
        writeRecords = null
        weekItems = null
        monthItems = null
        weekManageItems = null
        monthManageItems = null
        summaryItem = null
        weekTemplates = null
        monthTemplates = null
    }
}
