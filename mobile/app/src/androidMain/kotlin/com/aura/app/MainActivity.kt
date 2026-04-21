package com.aura.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aura.core.data.api.AuraApiClient
import com.aura.core.domain.model.GeoLocation
import com.aura.core.navigation.AuraNavigation
import com.aura.core.ui.theme.AuraTheme
import com.aura.feature.chat.ChatDocumentAttachment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.android.ext.android.inject
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    private val apiClient: AuraApiClient by inject()
    private var pendingDocumentContinuation: CancellableContinuation<ChatDocumentAttachment?>? = null
    private var pendingLocationContinuation: CancellableContinuation<GeoLocation?>? = null
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val continuation = pendingDocumentContinuation
        pendingDocumentContinuation = null
        if (continuation == null || !continuation.isActive) return@registerForActivityResult

        if (uri == null) {
            continuation.resume(null)
            return@registerForActivityResult
        }

        runCatching {
            val mimeType = contentResolver.getType(uri)
            val fileName = resolveDisplayName(uri) ?: "document"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            if (bytes.isEmpty()) null else ChatDocumentAttachment(fileName = fileName, bytes = bytes, mimeType = mimeType)
        }.onSuccess { document ->
            continuation.resume(document)
        }.onFailure {
            continuation.resume(null)
        }
    }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val continuation = pendingLocationContinuation
            pendingLocationContinuation = null

            if (continuation == null || !continuation.isActive) return@registerForActivityResult

            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (!granted) {
                continuation.resume(null)
                return@registerForActivityResult
            }

            fetchCurrentLocation { location ->
                continuation.resume(location)
            }
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuraNavigation(
                        apiClient = apiClient,
                        pickDocument = { pickDocument() },
                        requestUserLocation = { requestUserLocation() }
                    )
                }
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return null
    }

    private suspend fun pickDocument(): ChatDocumentAttachment? = suspendCancellableCoroutine { continuation ->
        pendingDocumentContinuation?.let { existing ->
            if (existing.isActive) {
                existing.resume(null)
            }
        }

        pendingDocumentContinuation = continuation
        openDocumentLauncher.launch(
            arrayOf(
                "application/pdf",
                "text/plain",
                "text/markdown",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        )

        continuation.invokeOnCancellation {
            if (pendingDocumentContinuation === continuation) {
                pendingDocumentContinuation = null
            }
        }
    }

    private suspend fun requestUserLocation(): GeoLocation? = suspendCancellableCoroutine { continuation ->
        if (isProbablyEmulator()) {
            continuation.resume(
                GeoLocation(
                    latitude = 55.7558,
                    longitude = 37.6176
                )
            )
            return@suspendCancellableCoroutine
        }

        pendingLocationContinuation?.let { existing ->
            if (existing.isActive) {
                existing.resume(null)
            }
        }

        pendingLocationContinuation = continuation

        if (hasLocationPermission()) {
            fetchCurrentLocation { location ->
                if (pendingLocationContinuation === continuation) {
                    pendingLocationContinuation = null
                }
                continuation.resume(location)
            }
        } else {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        continuation.invokeOnCancellation {
            if (pendingLocationContinuation === continuation) {
                pendingLocationContinuation = null
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun fetchCurrentLocation(onResult: (GeoLocation?) -> Unit) {
        if (!hasLocationPermission()) {
            onResult(null)
            return
        }

        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { currentLocation ->
                if (currentLocation != null) {
                    onResult(GeoLocation(latitude = currentLocation.latitude, longitude = currentLocation.longitude))
                    return@addOnSuccessListener
                }

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLocation ->
                        onResult(
                            lastLocation?.let {
                                GeoLocation(latitude = it.latitude, longitude = it.longitude)
                            }
                        )
                    }
                    .addOnFailureListener {
                        onResult(null)
                    }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT ?: ""
        val model = Build.MODEL ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        val brand = Build.BRAND ?: ""
        val device = Build.DEVICE ?: ""
        val product = Build.PRODUCT ?: ""

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator", ignoreCase = true) ||
            model.contains("Emulator", ignoreCase = true) ||
            model.contains("Android SDK built for x86", ignoreCase = true) ||
            manufacturer.contains("Genymotion", ignoreCase = true) ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product.contains("sdk", ignoreCase = true)
    }
}
