package dev.kodex.core.models

enum class Tier(val displayName: String, val colorToken: String) {
    JUNIOR("Junior Coder", "tier-junior"),
    SENIOR("Senior Coder", "tier-senior"),
    MASTER("Master", "tier-master"),
    GRANDMASTER("Grandmaster", "tier-grandmaster"),
}
