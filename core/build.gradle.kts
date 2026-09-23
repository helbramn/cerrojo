plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(17) }
dependencies { testImplementation(libs.junit) }

tasks.test {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
