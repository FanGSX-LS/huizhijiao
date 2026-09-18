package com.gxjzy.huizhijiao.map

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val longitude: Double = 0.0,
    val latitude: Double = 0.0,
    val address: String = "",
    val success: Boolean = false,
    val errorMsg: String = ""
)

object LocationHelper {

    private const val TAG = "LocationHelper"

    suspend fun locate(context: Context): LocationResult {
        return try {
            val location = getGmsLocation(context) ?: getPlatformLocation(context)
                ?: return LocationResult(errorMsg = "无法获取位置，请确保已开启定位权限和GPS")

            val bd = CoordTransform.wgs84ToBd09(location.longitude, location.latitude)
            val address = reverseGeocode(context, bd[1], bd[0])

            Log.d(TAG, "定位成功: wgs84(${location.longitude}, ${location.latitude}) -> bd09(${bd[0]}, ${bd[1]})")
            LocationResult(
                longitude = bd[0],
                latitude = bd[1],
                address = address,
                success = true
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "定位权限异常", e)
            LocationResult(errorMsg = "定位权限未授予")
        } catch (e: Exception) {
            Log.e(TAG, "定位异常", e)
            LocationResult(errorMsg = "定位失败: ${e.message}")
        }
    }

    private suspend fun getGmsLocation(context: Context): Location? {
        return try {
            val availability = GoogleApiAvailability.getInstance()
            val code = availability.isGooglePlayServicesAvailable(context)
            if (code != ConnectionResult.SUCCESS) {
                Log.d(TAG, "GMS不可用, code=$code")
                return null
            }

            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            suspendCancellableCoroutine { cont ->
                cont.invokeOnCancellation { cts.cancel() }
                try {
                    val request = CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build()
                    client.getCurrentLocation(request, cts.token)
                        .addOnSuccessListener { loc ->
                            Log.d(TAG, "GMS定位: $loc")
                            cont.resume(loc)
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "GMS定位失败", e)
                            cont.resume(null)
                        }
                } catch (e: SecurityException) {
                    Log.w(TAG, "GMS无权限", e)
                    cont.resume(null)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "GMS异常", e)
            null
        }
    }

    @Suppress("MissingPermission")
    private fun getPlatformLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (best == null) best = network
        else if (network != null && network.accuracy < best.accuracy) best = network
        Log.d(TAG, "平台定位: $best")
        return best
    }

    private fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.CHINA)
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (addresses.isNullOrEmpty()) {
                String.format("%.6f,%.6f", lng, lat)
            } else {
                val addr = addresses[0]
                val parts = mutableListOf<String>()
                addr.adminArea?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                addr.locality?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                addr.subLocality?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                addr.thoroughfare?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                addr.featureName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                if (parts.isNotEmpty()) parts.joinToString("") else addr.getAddressLine(0) ?: String.format("%.6f,%.6f", lng, lat)
            }
        } catch (e: Exception) {
            Log.w(TAG, "逆地理编码失败", e)
            String.format("%.6f,%.6f", lng, lat)
        }
    }
}
