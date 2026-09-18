package com.gxjzy.huizhijiao.utils

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
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
    val longitude: String,
    val latitude: String,
    val address: String
)

class LocationHelper(private val context: Context) {

    private val geocoder = Geocoder(context, Locale.CHINA)

    suspend fun fetchCurrentLocation(): LocationResult {
        val location = getGmsLocation() ?: getPlatformLocation()
            ?: throw IllegalStateException("无法获取位置，请确保已开启定位权限和GPS")

        val bd = CoordTransform.wgs84ToBd09(location.longitude, location.latitude)
        val address = reverseGeocode(bd[1], bd[0])

        return LocationResult(
            longitude = String.format("%.6f", bd[0]),
            latitude = String.format("%.6f", bd[1]),
            address = address
        )
    }

    private suspend fun getGmsLocation(): Location? {
        return try {
            val availability = GoogleApiAvailability.getInstance()
            val code = availability.isGooglePlayServicesAvailable(context)
            if (code != ConnectionResult.SUCCESS) return null

            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            suspendCancellableCoroutine { cont ->
                cont.invokeOnCancellation { cts.cancel() }
                try {
                    val request = CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build()
                    client.getCurrentLocation(request, cts.token)
                        .addOnSuccessListener { loc -> cont.resume(loc) }
                        .addOnFailureListener { cont.resume(null) }
                } catch (e: SecurityException) {
                    cont.resume(null)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("MissingPermission")
    private fun getPlatformLocation(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (best == null) best = network
        else if (network != null && network.accuracy < best.accuracy) best = network
        return best
    }

    private fun reverseGeocode(lat: Double, lng: Double): String {
        return try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (addresses.isNullOrEmpty()) return ""
            val addr = addresses[0]
            val parts = mutableListOf<String>()
            addr.adminArea?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            addr.locality?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            addr.subLocality?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            addr.thoroughfare?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            addr.featureName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            if (parts.isNotEmpty()) parts.joinToString("") else addr.getAddressLine(0) ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
