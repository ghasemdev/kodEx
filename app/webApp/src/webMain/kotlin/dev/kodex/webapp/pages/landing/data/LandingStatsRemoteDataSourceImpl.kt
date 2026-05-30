package dev.kodex.webapp.pages.landing.data

import dev.kodex.core.models.api.ApiEnvelope
import dev.kodex.core.models.landing.LandingStatsResponse
import dev.kodex.shared.coroutines.ioDispatcher
import dev.kodex.webapp.network.ApiRoutes
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single(binds = [LandingStatsRemoteDataSource::class])
class LandingStatsRemoteDataSourceImpl(
    private val client: HttpClient,
) : LandingStatsRemoteDataSource {
    override suspend fun fetchStats(): LandingStatsResponse = withContext(ioDispatcher) {
        client
            .get(ApiRoutes.LANDING_STATS)
            .body<ApiEnvelope<LandingStatsResponse>>()
            .data
    }
}
