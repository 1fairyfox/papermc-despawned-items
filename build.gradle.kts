import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.0"
    id("org.jetbrains.dokka") version "2.2.0"
}

group = "com.popupmc"
version = file("VERSION").readText().trim()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    // Paper API for Minecraft 1.21.11 (latest 1.21 patch). Targeting the 1.21 line
    // (not the newer 26.x) so the MockBukkit test framework — which supports 1.21.x
    // but not 26.x yet — is available; a 1.21-built plugin still loads on 26.1
    // servers (Paper forward-compat). compileOnly: the server provides it at runtime.
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // The Kotlin standard library is shaded into the plugin jar (see shadowJar
    // below) so the plugin is self-contained on a plain Paper server.
    implementation(kotlin("stdlib"))

    // --- Testing ---
    // MockBukkit mocks a live Paper 1.21 server for unit/integration tests (it
    // supports the 1.21 line but not the newer 26.x — a key reason for the target).
    // It also brings the Paper API and JUnit 5 onto the test classpath transitively.
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.110.0")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // The Paper API is compileOnly for main; tests need it on their own classpath.
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

kotlin {
    // Minecraft 1.21.x runs on Java 21; compile and target that so the jar loads on
    // any 1.21+ (including 26.1) server. Gradle auto-provisions JDK 21 via the
    // foojay resolver (see settings.gradle.kts) where it isn't already installed.
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

// API docs (Dokka), re-skinned to wear the fairyfox palette + a way-home link so the
// generated site reads as a page of fairyfox.io. Build with `./gradlew dokkaGenerate`.
dokka {
    moduleName.set("DespawnedItems")
    pluginsConfiguration.html {
        customStyleSheets.from(layout.projectDirectory.file("docs-theme/dokka-fairyfox.css"))
        customAssets.from(layout.projectDirectory.file("assets/icon.png"))
        footerMessage.set("Fairy Fox · fairyfox.io")
        homepageLink.set("https://fairyfox.io/")
    }
}

tasks {
    // `gradle build` produces the shaded, runnable plugin jar.
    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        // No classifier: the shaded jar IS the plugin artifact.
        archiveClassifier.set("")
        // The Kotlin stdlib is shaded whole (no minimize) so runtime-only
        // stdlib classes can't be stripped out on a server we can't test here.
    }

    jar {
        // Avoid emitting an unused plain jar alongside the shaded one.
        enabled = false
    }

    test {
        // JUnit 5 (Jupiter) is the test platform. `build` runs `check` → `test`, so
        // the suite gates every build.
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
