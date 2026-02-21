import java.util.Properties

plugins {
    java
}

group = "com.ivillager"

val versionFile = file("version.properties")

fun readVersion(): String {
    if (!versionFile.exists()) return "1.0.0"
    val p = Properties().apply { versionFile.inputStream().use { load(it) } }
    val major = (p.getProperty("major", "1")).toIntOrNull() ?: 1
    val minor = (p.getProperty("minor", "0")).toIntOrNull() ?: 0
    val build = (p.getProperty("build", "0")).toIntOrNull() ?: 0
    return "$major.$minor.$build"
}

fun writeNextVersion() {
    val p = Properties()
    if (versionFile.exists()) versionFile.inputStream().use { p.load(it) }
    var major = (p.getProperty("major", "1")).toIntOrNull() ?: 1
    var minor = (p.getProperty("minor", "0")).toIntOrNull() ?: 0
    var build = (p.getProperty("build", "0")).toIntOrNull() ?: 0
    build++
    if (build > 9) {
        build = 0
        minor++
    }
    p.setProperty("major", major.toString())
    p.setProperty("minor", minor.toString())
    p.setProperty("build", build.toString())
    versionFile.outputStream().use { p.store(it, "Auto-incremented on build. Build 0-9 then minor bumps.") }
}

version = readVersion()

tasks.named("build") {
    doLast {
        writeNextVersion()
        println("Version file updated for next build: ${readVersion()}")
    }
}
tasks.named("jar") {
    doLast {
        if (!gradle.startParameter.taskNames.contains("build")) {
            writeNextVersion()
            println("Version file updated for next build: ${readVersion()}")
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}
