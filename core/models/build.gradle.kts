plugins {
    id("kotlin-kmp-convention")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
        jvmTest.dependencies {
            implementation(libs.bundles.kotest)
        }
    }
}
