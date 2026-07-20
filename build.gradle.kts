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
    // Paper API for Minecraft 26.1.2 (build #74, the current stable line).
    // compileOnly: the server provides it at runtime.
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")

    // The Kotlin standard library is shaded into the plugin jar (see shadowJar
    // below) so the plugin is self-contained on a plain Paper server.
    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
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
}
