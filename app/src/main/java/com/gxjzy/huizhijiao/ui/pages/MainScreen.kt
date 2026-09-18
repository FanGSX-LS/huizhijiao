package com.gxjzy.huizhijiao.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.gxjzy.huizhijiao.ui.tabs.*
import com.gxjzy.huizhijiao.ui.components.AppButton
import com.gxjzy.huizhijiao.ui.components.AppScaffold
import com.gxjzy.huizhijiao.ui.components.LiquidBottomTab
import com.gxjzy.huizhijiao.ui.components.LiquidBottomTabs
import com.gxjzy.huizhijiao.ui.theme.*
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.gxjzy.huizhijiao.BRApp
import com.gxjzy.huizhijiao.api.ApiClient
import com.gxjzy.huizhijiao.ui.page.main.rememberMainPagerState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import coil3.compose.rememberAsyncImagePainter
import java.io.File
import android.net.Uri
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToWeeklyReport: () -> Unit,
    onNavigateToMonthlySummary: () -> Unit,
    onNavigateToInternshipSummary: () -> Unit,
    onNavigateToWeekManage: () -> Unit,
    onNavigateToMonthManage: () -> Unit,
    onNavigateToTemplate: () -> Unit,
    onNavigateToReportDetail: (type: String, id: String, title: String) -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val prefs = BRApp.instance.prefs
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var showExpiredDialog by remember { mutableStateOf(false) }
    var expiredEndDate by remember { mutableStateOf("") }
    val bgImage by prefs.backgroundImage.distinctUntilChanged().collectAsState(initial = null)
    val backdrop = rememberLayerBackdrop()

    val pagerState = rememberPagerState(pageCount = { 4 })
    val mainPagerState = rememberMainPagerState(pagerState)
    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
        currentTab = mainPagerState.selectedPage
    }

    LaunchedEffect(Unit) {
        if (!ApiClient.expiredDialogShownThisSession) {
            val user = ApiClient.fetchMe() ?: ApiClient.getCachedUser()
            if (user != null && user.advancedModeExpired) {
                val advancedOn = prefs.advancedMode.first()
                if (advancedOn) {
                    prefs.saveAdvancedMode(false)
                }
                expiredEndDate = user.advancedModeEnd
                ApiClient.expiredDialogShownThisSession = true
                showExpiredDialog = true
            }
        }
    }

    BackHandler(enabled = mainPagerState.selectedPage != 0) {
        mainPagerState.animateToPage(0)
    }

    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val unselectedColor = if (isLightTheme) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.7f) else Color(0xFF1E1E1E).copy(alpha = 0.7f)

    val tabItems = listOf(
        Triple(MiuixIcons.Home, "首页", 0),
        Triple(MiuixIcons.Ok, "签到", 1),
        Triple(MiuixIcons.ListView, "管理", 2),
        Triple(MiuixIcons.Contacts, "我的", 3)
    )

    AppScaffold(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.layerBackdrop(backdrop).fillMaxSize()) {
                val currentBgImage = bgImage
                if (currentBgImage != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = Uri.fromFile(File(currentBgImage))),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF3F3F3))
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (page) {
                        0 -> HomeTab()
                        1 -> CheckInTab()
                        2 -> ManageTab(
                            onNavigateToWeekManage = onNavigateToWeekManage,
                            onNavigateToMonthManage = onNavigateToMonthManage,
                            onNavigateToReportDetail = onNavigateToReportDetail,
                            onNavigateToWeeklyReport = onNavigateToWeeklyReport,
                            onNavigateToMonthlySummary = onNavigateToMonthlySummary,
                            onNavigateToInternshipSummary = onNavigateToInternshipSummary
                        )
                        3 -> MyTab(
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToTemplate = onNavigateToTemplate,
                            onLogout = onLogout
                        )
                    }
                }
            }

            LiquidBottomTabs(
                selectedTabIndex = { mainPagerState.selectedPage },
                onTabSelected = { mainPagerState.animateToPage(it) },
                backdrop = backdrop,
                tabsCount = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 12.dp)
            ) {
                tabItems.forEach { (icon, label, index) ->
                    val selected = mainPagerState.selectedPage == index
                    val color = if (selected) accentColor else unselectedColor
                    LiquidBottomTab(onClick = { mainPagerState.animateToPage(index) }) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(top = 4.dp))
                        BasicText(label, style = TextStyle(color = color, fontSize = 10.sp))
                    }
                }
            }

            OverlayDialog(
                show = showExpiredDialog,
                onDismissRequest = { showExpiredDialog = false },
                title = "高级模式已到期",
            ) {
                Text("您的高级模式已于 ${if (expiredEndDate.isNotEmpty()) expiredEndDate else "未知日期"} 到期，已自动关闭。如需续期，请联系管理员。")
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    AppButton(onClick = { showExpiredDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("我知道了") }
                }
            }
        }
    }
}
