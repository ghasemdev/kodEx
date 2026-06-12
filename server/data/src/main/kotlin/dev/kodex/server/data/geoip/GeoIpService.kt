package dev.kodex.server.data.geoip

import com.maxmind.geoip2.DatabaseReader
import dev.kodex.core.config.ConfigQualifier
import java.io.File
import java.net.InetAddress
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

data class GeoResult(val countryCode: String?, val city: String?)

@Single
class GeoIpService(
    @Named(ConfigQualifier.Geo.DB_PATH) dbPath: String,
) {
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
