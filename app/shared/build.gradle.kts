plugins {
    id("kotlin-kmp-convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
        }
    }
}
