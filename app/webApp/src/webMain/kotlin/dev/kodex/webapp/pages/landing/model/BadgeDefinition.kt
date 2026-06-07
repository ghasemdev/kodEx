package dev.kodex.webapp.pages.landing.model

data class BadgeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val unlockCondition: String,
    val iconPath: String,
) {
    init {
        require(id.matches(Regex("^[a-z0-9-]+$"))) {
            $$"id '$$id' must match ^[a-z0-9-]+$"
        }
        require(iconPath.matches(Regex("^icons/badges/[a-z0-9/.-]+\\.png$"))) {
            $$"iconPath '$$iconPath' must match ^icons/badges/[a-z0-9/.-]+\\.png$"
        }
        require(name.isNotBlank()) { "name must not be blank" }
        require(description.isNotBlank()) { "description must not be blank" }
        require(unlockCondition.isNotBlank()) { "unlockCondition must not be blank" }
    }
}

object Badges {
    @Suppress("PropertyName", "RedundantSuppression")
    val all: List<BadgeDefinition> = listOf(
        BadgeDefinition(
            id = "kotlin-ninja",
            name = "Kotlin Ninja",
            description = "Master of Kotlin syntax and idioms.",
            unlockCondition = "Solve 50 Kotlin problems.",
            iconPath = "icons/badges/kotlin-ninja.png",
        ),
        BadgeDefinition(
            id = "android-architect",
            name = "Android Architect",
            description = "Design scalable Android applications.",
            unlockCondition = "Solve 30 Android architecture problems.",
            iconPath = "icons/badges/android-architect.png",
        ),
        BadgeDefinition(
            id = "night-owl",
            name = "Night Owl",
            description = "Code when the world is asleep.",
            unlockCondition = "Submit a solution between midnight and 4 AM.",
            iconPath = "icons/badges/night-owl.png",
        ),
        BadgeDefinition(
            id = "steady-hand",
            name = "Steady Hand",
            description = "Consistent practice builds mastery.",
            unlockCondition = "Maintain a 7-day solving streak.",
            iconPath = "icons/badges/steady-hand.png",
        ),
        BadgeDefinition(
            id = "bug-hunter",
            name = "Bug Hunter",
            description = "Find and fix tricky edge cases.",
            unlockCondition = "Pass all hidden edge-case tests in 10 problems.",
            iconPath = "icons/badges/bug-hunter.png",
        ),
        BadgeDefinition(
            id = "speed-demon",
            name = "Speed Demon",
            description = "Lightning-fast solutions under pressure.",
            unlockCondition = "Solve a problem in under 60 seconds.",
            iconPath = "icons/badges/speed-demon.png",
        ),
        BadgeDefinition(
            id = "creative-chaos",
            name = "Creative Chaos",
            description = "Unconventional approaches that still pass.",
            unlockCondition = "Solve a problem using an unexpected algorithm.",
            iconPath = "icons/badges/creative-chaos.png",
        ),
        BadgeDefinition(
            id = "hello-world",
            name = "Hello World",
            description = "Every journey begins with a single step.",
            unlockCondition = "Solve your first problem.",
            iconPath = "icons/badges/hello-world.png",
        ),
    )
}
