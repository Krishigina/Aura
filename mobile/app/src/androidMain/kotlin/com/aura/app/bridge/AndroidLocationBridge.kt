package com.aura.app.bridge

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.aura.core.data.api.model.WeatherCoordinates
import com.aura.feature.home.HomeLocationBridge

internal fun ComponentActivity.deliverWeatherLocationPermissionResult(permissions: Map<String, Boolean>) {
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    HomeLocationBridge.deliver(if (fineGranted || coarseGranted) readLastKnownWeatherCoordinates() else null)
}

internal fun ComponentActivity.requestWeatherLocation(
    locationPermissionLauncher: ActivityResultLauncher<Array<String>>,
) {
    if (hasWeatherLocationPermission()) {
        HomeLocationBridge.deliver(readLastKnownWeatherCoordinates())
    } else {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
}

private fun ComponentActivity.hasWeatherLocationPermission(): Boolean {
    val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return hasFine || hasCoarse
}

private fun ComponentActivity.readLastKnownWeatherCoordinates(): WeatherCoordinates? {
    if (!hasWeatherLocationPermission()) return null

    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = locationManager.getProviders(true)
    val bestLocation = providers
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull(Location::getTime)

    return bestLocation?.let { location ->
        WeatherCoordinates(latitude = location.latitude, longitude = location.longitude)
    }
}
