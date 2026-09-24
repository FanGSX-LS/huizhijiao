package com.gxjzy.huizhijiao.utils

import com.gxjzy.huizhijiao.BRApp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubAsset>?
)

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("size") val size: Long
)

object UpdateChecker {

    private const val REPO_API = "https://api.github.com/repos/FanGSX-LS/huizhijiao/releases/latest"
    private val gson = Gson()

    suspend fun checkForUpdate(): UpdateResult? = withContext(Dispatchers.IO) {
        try {
            val json = URL(REPO_API).readText()
            val release = gson.fromJson(json, GitHubRelease::class.java) ?: return@withContext null
            val currentVersion = getCurrentVersionName()
            if (release.tagName != currentVersion && isNewer(release.tagName, currentVersion)) {
                val apkAsset = release.assets?.firstOrNull { it.name.endsWith(".apk") }
                UpdateResult(
                    latestVersion = release.tagName,
                    releaseNotes = release.body ?: "",
                    downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl,
                    apkSize = apkAsset?.size ?: 0L,
                    releaseUrl = release.htmlUrl
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getCurrentVersionName(): String {
        return try {
            BRApp.instance.packageManager.getPackageInfo(BRApp.instance.packageName, 0).versionName ?: "0"
        } catch (_: Exception) {
            "0"
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.removePrefix("v").removePrefix("V")
        val l = local.removePrefix("v").removePrefix("V")
        val rParts = r.split(Regex("[.\\-]")).mapNotNull { it.toIntOrNull() }
        val lParts = l.split(Regex("[.\\-]")).mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(rParts.size, lParts.size)) {
            val rv = rParts.getOrElse(i) { 0 }
            val lv = lParts.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}

data class UpdateResult(
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long,
    val releaseUrl: String
)
