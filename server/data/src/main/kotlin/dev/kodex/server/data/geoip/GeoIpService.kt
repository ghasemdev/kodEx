package dev.kodex.server.data.geoip

import com.maxmind.geoip2.DatabaseReader
import java.io.File
import java.net.InetAddress

data class GeoResult(val countryCode: String?, val city: String?)

class GeoIpService(dbPath: String) {
    private val reader: DatabaseReader? = runCatching {
        DatabaseReader.Builder(File(dbPath)).build()
    }.getOrNull()

    fun lookup(ip: String): GeoResult? {
        reader ?: return null
        return try {
            val inet = InetAddress.getByName(ip)
            val response = reader.city(inet)
            GeoResult(
                countryCode = response.country().isoCode(),
                city = response.city().name(),
            )
        } catch (_: Exception) {
            null
        }
    }
}
