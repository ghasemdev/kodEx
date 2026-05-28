import java.util.Properties
import org.gradle.api.Project

class Config private constructor(
    private val key: String,
) {
    private var envKey: String? = null
    private var propertyKey: String? = null

    fun env(envKey: String) = apply {
        this.envKey = envKey
    }

    fun property(propertyKey: String) = apply {
        this.propertyKey = propertyKey
    }

    fun resolve(project: Project): String? {
        // 1. CLI (-Pkey=value)
        project.findProperty(key)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        // 2. ENV
        val env = envKey ?: key.uppercase()
        System.getenv(env)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        // 3. local.properties
        val file = project.rootProject.file("local.properties")

        if (file.exists()) {
            val props = Properties()
            file.inputStream().use { props.load(it) }

            val prop = propertyKey ?: key
            props.getProperty(prop)
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        return null
    }

    companion object {
        fun get(key: String) = Config(key)
    }
}
