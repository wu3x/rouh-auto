package org.wpfy.carlauncher.shell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.data.DateComponents
import com.batoulapps.adhan.PrayerTimes
import kotlinx.coroutines.delay
import org.wpfy.carlauncher.design.theme.CarColors
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatusArea(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(formattedTime()) }
    var nextPrayer by remember { mutableStateOf<PrayerDisplay?>(null) }

    LaunchedEffect(context) {
        while (true) {
            currentTime = formattedTime()
            nextPrayer = calculateNextPrayer(context)
            delay(60_000L)
        }
    }

    Column(
        modifier = modifier.width(86.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = currentTime,
            color = CarColors.TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(7.dp))
        if (nextPrayer != null) {
            Text(
                text = nextPrayer!!.name,
                color = Color(0xFFB9C9DB),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = nextPrayer!!.time,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private data class PrayerDisplay(val name: String, val time: String)
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun formattedTime(): String = LocalTime.now().format(TimeFormatter)

@Suppress("MissingPermission")
private fun calculateNextPrayer(context: Context): PrayerDisplay? {
    val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val last = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .filter { System.currentTimeMillis() - it.time < 7L * 24 * 60 * 60 * 1000 }
        .maxByOrNull { it.time } ?: return null
    val coordinates = Coordinates(last.latitude, last.longitude)
    val parameters: CalculationParameters = CalculationMethod.UMM_AL_QURA.parameters
    val now = Date()
    val calendar = Calendar.getInstance()
    val today = DateComponents.from(calendar.time)
    val todayTimes = PrayerTimes(coordinates, today, parameters)
    val prayers = listOf(
        "صلاة الفجر" to todayTimes.fajr,
        "صلاة الظهر" to todayTimes.dhuhr,
        "صلاة العصر" to todayTimes.asr,
        "صلاة المغرب" to todayTimes.maghrib,
        "صلاة العشاء" to todayTimes.isha
    )
    val upcoming = prayers.firstOrNull { it.second.after(now) } ?: run {
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrow = PrayerTimes(coordinates, DateComponents.from(calendar.time), parameters)
        "الفجر" to tomorrow.fajr
    }
    val time = SimpleDateFormat("h:mm", Locale.US).format(upcoming.second) +
        if (Calendar.getInstance().apply { time = upcoming.second }.get(Calendar.AM_PM) == Calendar.AM) " ص" else " م"
    return PrayerDisplay(upcoming.first, time)
}
