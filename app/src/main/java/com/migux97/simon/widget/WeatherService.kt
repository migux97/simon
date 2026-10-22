package com.migux97.simon.widget

import com.migux97.simon.data.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Place(val name: String, val region: String, val country: String, val lat: Double, val lon: Double) {
    val label: String get() = listOf(name, region, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

/** Clima gratis y sin clave con Open-Meteo. */
object WeatherService {

    private fun get(url: String): String? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "Simon-Android")
        try {
            if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        null
    }

    suspend fun searchCity(query: String): List<Place> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val body = get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=es&format=json")
            ?: return@withContext emptyList()
        try {
            val arr = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Place(
                    name = o.optString("name"),
                    region = o.optString("admin1"),
                    country = o.optString("country"),
                    lat = o.optDouble("latitude"),
                    lon = o.optDouble("longitude")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetch(lat: Double, lon: Double): WeatherInfo? = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=1"
        val body = get(url) ?: return@withContext null
        try {
            val root = JSONObject(body)
            val current = root.getJSONObject("current")
            val daily = root.getJSONObject("daily")
            WeatherInfo(
                temperature = current.getDouble("temperature_2m"),
                code = current.getInt("weather_code"),
                max = daily.getJSONArray("temperature_2m_max").getDouble(0),
                min = daily.getJSONArray("temperature_2m_min").getDouble(0),
                isDay = current.optInt("is_day", 1) == 1,
                fetchedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}
