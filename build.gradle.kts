import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.0"
    id("org.jetbrains.dokka") version "2.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.kover") version "0.9.9"
}

group = "io.fairyfox"
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

    // HikariCP for the JDBC connection pool. compileOnly: Paper loads it (and the JDBC
    // drivers) at runtime via the `libraries:` block in plugin.yml, so nothing DB-related
    // is shaded into the jar.
    compileOnly("com.zaxxer:HikariCP:5.1.0")

    // --- Testing ---
    // MockBukkit mocks a live Paper 1.21 server for unit/integration tests (it
    // supports the 1.21 line but not the newer 26.x — a key reason for the target).
    // It also brings the Paper API and JUnit 5 onto the test classpath transitively.
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.110.0")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // The Paper API is compileOnly for main; tests need it on their own classpath.
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // Database backend: HikariCP for the pool, SQLite driver to exercise JdbcLocationRepository.
    // In production these load at runtime via Paper's `libraries:` loader (see plugin.yml).
    testImplementation("com.zaxxer:HikariCP:5.1.0")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.49.1.0")
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

// Static analysis. Layers on the default rules (see config/detekt/detekt.yml); pre-existing
// findings are captured in the baseline so detekt gates only new issues. Runs on the
// JDK-21 Gradle daemon (see gradle/gradle-daemon-jvm.properties).
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    baseline = file("config/detekt/baseline.xml")
}

// API docs (Dokka), re-skinned to wear the fairyfox palette + a way-home link so the
// generated site reads as a page of fairyfox.io. Build with `./gradlew dokkaGenerate`.
dokka {
    moduleName.set("PaperMC Despawned Items")
    pluginsConfiguration.html {
        // Reference-body harmony: reproduce the fairyfox tokens onto Dokka's own design
        // variables (per docs-site standard 01-11 — tokens are reimplemented per stack).
        customStyleSheets.from(layout.projectDirectory.file("docs-theme/dokka-fairyfox.css"))
        customAssets.from(layout.projectDirectory.file("assets/icon.png"))
        // Shared chrome (masthead/subnav/footer/reader) is injected verbatim via the
        // FreeMarker template overrides in docs-theme/dokka-templates/ (chrome bundle,
        // see docs-theme/chrome/VERSION). This is the sanctioned "wear the chrome,
        // boundary the reference" adapter for a full-page generator.
        templatesDir.set(layout.projectDirectory.dir("docs-theme/dokka-templates"))
        footerMessage.set("Fairy Fox · fairyfox.io")
        homepageLink.set("https://fairyfox.io/")
    }
}

// Vendor the shared-chrome master assets (main.css + the three behaviour scripts) into
// the generated docs root so the templates can reference them via ${'$'}{pathToRoot}. The
// deployed site ships its own copies and renders with fairyfox.io offline (never hot-linked).
val vendorChromeAssets by tasks.registering(Copy::class) {
    from(layout.projectDirectory.dir("docs-theme/chrome")) {
        include("main.css", "reader.js", "nav.js", "coins.js")
    }
    into(layout.buildDirectory.dir("dokka/html"))
}
tasks.named("dokkaGenerate") { finalizedBy(vendorChromeAssets) }

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
