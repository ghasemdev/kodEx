package dev.kodex.server.api.auth.middleware

import dev.kodex.core.models.auth.Role
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route

class ForbiddenException(message: String = "Insufficient permissions.") : RuntimeException(message)

@Suppress("PropertyName", "RedundantSuppression")
val RoleGuardPlugin = createRouteScopedPlugin("RoleGuard", ::RoleGuardConfig) {
    val required = pluginConfig.roles
    onCall { call ->
        val principal = call.principal<JWTPrincipal>()
        val roleStr = principal?.payload?.getClaim("role")?.asString()
        val role = roleStr?.let { runCatching { Role.valueOf(it) }.getOrNull() }
        if (role == null || role !in required) throw ForbiddenException()
    }
}

class RoleGuardConfig {
    var roles: Set<Role> = emptySet()
}

fun Route.requireRole(vararg roles: Role) {
    install(RoleGuardPlugin) { this.roles = roles.toSet() }
}
