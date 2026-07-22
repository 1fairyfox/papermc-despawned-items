import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Markdown → HTML for the generated docs-site pages (changelog + rendered notes).
// Buildscript-classpath only — nothing ships in the plugin jar.
buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("com.vladsch.flexmark:flexmark-all:0.64.8") }
}

plugins {
    // Kotlin is held at 2.4.0 (not 2.4.10) deliberately: CodeQL 2.26.0 (2026-07-10)
    // supports Kotlin *up to 2.4.0*, and its extractor hard-rejects anything newer
    // ("Kotlin version 2.4.10 is too recent"). 2.4.0 keeps the full SAST scan alive;
    // bump only after CodeQL's supported range catches up.
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.0"
    id("org.jetbrains.dokka") version "2.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.kover") version "0.9.9"
    // JMH microbenchmarks (src/jmh/kotlin) — informational, NOT part of `check`.
    // Run with `./gradlew jmh`; CI publishes results weekly (bench.yml).
    id("me.champeau.jmh") version "0.7.3"
    // Mutation testing — informational, NOT part of `check`. Scoped to the pure core
    // (see pitest {} below); run with `./gradlew pitest`.
    id("info.solidsoft.pitest") version "1.19.0"
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
    compileOnly("com.zaxxer:HikariCP:7.1.0")

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
    testImplementation("com.zaxxer:HikariCP:7.1.0")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.53.2.1")
    // Real MySQL/MariaDB integration via Testcontainers (needs Docker; the tests disable
    // themselves cleanly where it's absent). Driver version matches plugin.yml `libraries:`.
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:mariadb:1.21.4")
    testRuntimeOnly("org.mariadb.jdbc:mariadb-java-client:3.5.9")
    // Surface Testcontainers/Hikari logs in test output (slf4j otherwise has no binding).
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.18")
    // Kotest property testing — generator-driven invariants (roundtrips, store ops).
    testImplementation("io.kotest:kotest-property:5.9.1")
    // checkAll is a suspend fun; runBlocking hosts it inside JUnit tests.
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
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

// Static analysis. Layers on the default rules (see config/detekt/detekt.yml). No
// baseline: every rule gates every build — pre-existing findings were fixed, not parked.
// Runs on the JDK-21 Gradle daemon (see gradle/gradle-daemon-jvm.properties).
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
}

// JMH: quick-but-honest defaults (results stabilize enough for trend tracking without
// hour-long runs). JSON results land in build/results/jmh/ for the CI artifact.
jmh {
    warmupIterations.set(2)
    iterations.set(3)
    fork.set(1)
    warmup.set("1s")
    timeOnIteration.set("1s")
    resultFormat.set("JSON")
}

// Pitest mutation testing, scoped to the PURE core (store/locations/config parsing/
// reward logic) where mutations are meaningful and the runs stay fast. Deliberately
// excludes MockBukkit-driven suites — mutating server-API plumbing under a mock
// yields noise, not signal. Informational: not wired into `check`.
pitest {
    junit5PluginVersion.set("1.2.1")
    targetClasses.set(
        listOf(
            "io.fairyfox.papermc.despawneditems.location.DespawnLocation*",
            "io.fairyfox.papermc.despawneditems.location.BlockKey*",
            "io.fairyfox.papermc.despawneditems.location.LocationStore*",
            "io.fairyfox.papermc.despawneditems.RecycleProgress*",
            "io.fairyfox.papermc.despawneditems.config.CommandSettings*",
        ),
    )
    // Pure suites only: infrastructure-backed tests (containers, MockBukkit boots,
    // 1M-op benches) time mutation minions out without adding signal.
    targetTests.set(
        listOf(
            "io.fairyfox.papermc.despawneditems.location.DespawnLocationTest",
            "io.fairyfox.papermc.despawneditems.location.LocationStoreTest",
            "io.fairyfox.papermc.despawneditems.property.PropertyInvariantsTest",
            "io.fairyfox.papermc.despawneditems.RecycleProgressTest",
            "io.fairyfox.papermc.despawneditems.config.CommandSettingsTest",
        ),
    )
    threads.set(4)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
}

// Coverage gate: `check` (and therefore `build`) fails if line coverage regresses below
// 90%. The suite currently sits ~95%; the headroom covers the genuinely untestable
// remainder (MySQL connect paths need a live server; a few MockBukkit-unmodelable
// tile-entity copy branches; defensive lateinit guards).
kover {
    reports {
        verify {
            rule("Line coverage must stay at or above 90%") {
                minBound(90)
            }
        }
    }
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
        // Way back out of the boundaried API zone → the project's own overview
        // (docs-site 06: "always give a way back to the themed docs").
        homepageLink.set("https://fairyfox.io/papermc-despawned-items/")
    }
}

// ── Docs site (fairyfox docs-site standard, Case A) ─────────────────────────────
// build/docs-site = hand-authored chrome pages at the root (overview landing =
// DEFAULT page, notes/, tutorials, changelog, downloads) with the Dokka API
// reference boundaried under /api/, reached via the subnav "API" item. Changelog
// and the Notes section are GENERATED from the living notes/ tree (docs-site 06:
// don't hand-maintain what a generator can produce). Assets vendored, incl. the
// fox brand icon (self-hosted-assets — no hot-links).

// Chrome assets into the Dokka tree, so API pages resolve them via ${'$'}{pathToRoot}.
val vendorChromeAssets by tasks.registering(Copy::class) {
    from(layout.projectDirectory.dir("docs-theme/chrome")) {
        include("main.css", "reader.js", "nav.js", "coins.js", "fox.png")
    }
    into(layout.buildDirectory.dir("dokka/html"))
}
tasks.named("dokkaGenerate") { finalizedBy(vendorChromeAssets) }

// Assemble the hand-authored pages + generated changelog + rendered notes tree.
val renderDocsSite by tasks.registering {
    group = "documentation"
    description = "Render the chrome shell pages, changelog and notes into build/docs-pages"
    val pagesDir = layout.projectDirectory.dir("docs-theme/pages")
    val notesDir = layout.projectDirectory.dir("notes")
    val outDirProv = layout.buildDirectory.dir("docs-pages")
    inputs.dir(pagesDir)
    inputs.dir(notesDir)
    outputs.dir(outDirProv)
    doLast {
        val shell = pagesDir.file("_shell.html").asFile.readText()
        val out = outDirProv.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        val exts = listOf(com.vladsch.flexmark.ext.tables.TablesExtension.create())
        val parser =
            com.vladsch.flexmark.parser.Parser.builder().apply {
                extensions(exts)
            }.build()
        val renderer =
            com.vladsch.flexmark.html.HtmlRenderer.builder().apply {
                extensions(exts)
            }.build()

        // Render md → html, rewriting relative .md links to .html so inter-note links work.
        fun md(text: String): String =
            renderer.render(parser.parse(text))
                .replace(Regex("href=\"(?!https?:)([^\"]+?)\\.md(#[^\"]*)?\"")) { m ->
                    "href=\"${m.groupValues[1]}.html${m.groupValues[2]}\""
                }

        fun page(
            root: String,
            title: String,
            desc: String,
            active: String?,
            read: Boolean,
            content: String,
        ): String {
            var h =
                shell
                    .replace("{{FF_ROOT}}", root)
                    .replace("{{FF_TITLE}}", title)
                    .replace("{{FF_DESC}}", desc)
                    .replace("{{FF_HTML_ATTRS}}", if (read) " data-read" else "")
                    .replace("{{FF_CONTENT}}", content)
            h =
                h.replace("{{ACTIVE_HOME}}", if (active == "HOME") " active" else "")
                    .replace("{{ARIA_HOME}}", if (active == "HOME") " aria-current=\"page\"" else "")
            for (t in listOf("OVERVIEW", "NOTES", "SYSTEMS", "REFERENCE", "TUTORIALS", "CHANGELOG", "DOWNLOAD", "LEGAL")) {
                h =
                    h.replace(
                        "{{ACTIVE_$t}}",
                        if (t == active) " class=\"active\" aria-current=\"page\"" else "",
                    )
            }
            return h
        }

        // 1 · The three static pages (bodies in docs-theme/pages/content/).
        fun body(name: String) = pagesDir.file("content/$name").asFile.readText()
        File(out, "index.html").writeText(
            page(
                "",
                "PaperMC Despawned Items",
                "A Paper plugin that relocates despawning items into nearby containers, " +
                    "cookers, entities, or empty space instead of deleting them.",
                "HOME",
                false,
                body("index.html"),
            ),
        )
        File(out, "tutorials.html").writeText(
            page(
                "",
                "Tutorials · PaperMC Despawned Items",
                "Install, configure, and use the PaperMC Despawned Items plugin.",
                "TUTORIALS",
                true,
                body("tutorials.html"),
            ),
        )
        File(out, "downloads.html").writeText(
            page(
                "",
                "Download · PaperMC Despawned Items",
                "Download the PaperMC Despawned Items plugin jar — latest release and all versions.",
                "DOWNLOAD",
                false,
                body("downloads.html"),
            ),
        )

        // 1b · Legal pages (legal-docs standard — mandatory, self-hosted, code-accurate).
        // Emitted at legal/<name>/index.html so the chrome footer's pretty URLs
        // (…/legal/privacy/) resolve on Pages; plus the legal/ index (the subnav door).
        File(out, "legal").mkdirs()
        File(out, "legal/index.html").writeText(
            page(
                "../",
                "Legal · PaperMC Despawned Items",
                "Privacy, Terms, and Cookies for PaperMC Despawned Items — self-hosted and accurate to the code.",
                "LEGAL",
                false,
                body("legal/index.html"),
            ),
        )
        for ((slug, title) in listOf(
            "privacy" to "Privacy Policy",
            "terms" to "Terms & Conditions",
            "cookies" to "Cookies Policy",
        )) {
            val dir = File(out, "legal/$slug")
            dir.mkdirs()
            File(dir, "index.html").writeText(
                page(
                    "../../",
                    "$title · PaperMC Despawned Items",
                    "$title for PaperMC Despawned Items.",
                    "LEGAL",
                    true,
                    body("legal/$slug.html"),
                ),
            )
        }

        // 2 · Changelog — generated from notes/version/*.md, newest month first.
        val months =
            notesDir.dir("version").asFile.listFiles { f -> f.extension == "md" }
                ?.sortedByDescending { it.name } ?: emptyList()
        val changelog =
            buildString {
                append("<h1>Changelog</h1>\n<p>Every release in plain English, newest first — ")
                append("generated from the project's living notes (<code>notes/version/</code>).</p>\n")
                months.forEach { append(md(it.readText())) }
            }
        File(out, "changelog.html").writeText(
            page(
                "",
                "Changelog · PaperMC Despawned Items",
                "Release-by-release changelog for PaperMC Despawned Items.",
                "CHANGELOG",
                true,
                changelog,
            ),
        )

        // 3 · Notes — the living notes/ tree rendered under notes/ (docs-site 06:
        // one Notes door, a landing with intro + section cards, a sidebar listing
        // EVERY note organised by section). READMEs are RENDERED (so inter-note links
        // to them resolve — no 404s) but EXCLUDED from the sidebar (they're section
        // overviews, which the standard keeps out of the note list); the root
        // notes/README.md is the landing's own intro.
        val allNoteFiles =
            notesDir.asFile.walkTopDown()
                .filter { it.isFile && it.extension == "md" }
                .map { it.relativeTo(notesDir.asFile).invariantSeparatorsPath }
                .sorted().toList()
        val renderFiles = allNoteFiles.filter { it != "README.md" }
        val noteFiles = allNoteFiles.filter { !it.endsWith("README.md") }
        val sections =
            linkedMapOf(
                "Status & Changelog" to { p: String -> !p.contains("/") },
                "Context" to { p: String -> p.startsWith("context/") },
                "System Map" to { p: String -> p.startsWith("systems/") },
                "Decisions" to { p: String -> p.startsWith("decisions/") },
                "Plans" to { p: String -> p.startsWith("plans/") },
                "Reference" to { p: String -> p.startsWith("reference/") },
                "Session Logs" to { p: String -> p.startsWith("sessions/") },
                "Changelog Months" to { p: String -> p.startsWith("version/") },
                "Fairyfox Reports" to { p: String -> p.startsWith("fairyfox-reports/") },
            )

        fun sidebar(
            root: String,
            current: String?,
        ): String =
            buildString {
                append("<aside class=\"notes-side\" aria-label=\"All notes\">")
                append("<a href=\"${root}notes/index.html\"><strong>Notes home</strong></a>")
                for ((name, match) in sections) {
                    val inSection = noteFiles.filter { match(it) && !(name == "Status & Changelog" && it.contains("/")) }
                    if (inSection.isEmpty()) continue
                    append("<h4>$name</h4>")
                    for (p in inSection) {
                        val href = root + "notes/" + p.removeSuffix(".md") + ".html"
                        val label = p.substringAfterLast("/").removeSuffix(".md")
                        val cls = if (p == current) " class=\"current\"" else ""
                        append("<a href=\"$href\"$cls>$label</a>")
                    }
                }
                append("</aside>")
            }
        // Landing: root-note intro (notes/README.md) + section cards.
        val landingIntro = md(notesDir.file("README.md").asFile.readText())
        val landing =
            buildString {
                append("<div class=\"notes-layout\">")
                append(sidebar("../", null))
                append("<article class=\"notes-article\"><h1>Notes</h1>")
                append("<p>The project's living documentation — start with Status, then explore by section.</p>")
                append(landingIntro)
                append("</article></div>")
            }
        val notesOut = File(out, "notes")
        notesOut.mkdirs()
        File(notesOut, "index.html").writeText(
            page(
                "../",
                "Notes · PaperMC Despawned Items",
                "Living documentation for PaperMC Despawned Items — status, decisions, plans, session logs.",
                "NOTES",
                false,
                landing,
            ),
        )

        // Section landing pages — the subnav's Systems / Reference doors (parity with the
        // sibling project's broken-out Notes sections). Each lists that section's notes; only
        // emitted when the section actually has notes, so a subnav pill never points at a 404.
        val sectionDoors =
            listOf(
                Triple("systems", "Systems", "SYSTEMS"),
                Triple("reference", "Reference", "REFERENCE"),
            )
        for ((dir, sectionName, activeKey) in sectionDoors) {
            val inSection = noteFiles.filter { it.startsWith("$dir/") }
            if (inSection.isEmpty()) continue
            val list =
                buildString {
                    append("<ul>")
                    for (p in inSection) {
                        val href = "../../notes/" + p.removeSuffix(".md") + ".html"
                        val label = p.substringAfterLast("/").removeSuffix(".md")
                        append("<li><a href=\"$href\">$label</a></li>")
                    }
                    append("</ul>")
                }
            val body =
                "<div class=\"notes-layout\">" + sidebar("../../", null) +
                    "<article class=\"notes-article\"><h1>$sectionName</h1>" +
                    "<p>Notes in the $sectionName section — see the full list in the sidebar.</p>" +
                    list + "</article></div>"
            val secDir = File(notesOut, dir)
            secDir.mkdirs()
            File(secDir, "index.html").writeText(
                page(
                    "../../",
                    "$sectionName · Notes · PaperMC Despawned Items",
                    "$sectionName notes for PaperMC Despawned Items.",
                    activeKey,
                    false,
                    body,
                ),
            )
        }
        // Every note page, data-read, with the full sidebar.
        for (p in renderFiles) {
            val depth = p.count { it == '/' } + 1 // +1 for the notes/ dir itself
            val root = "../".repeat(depth)
            val target = File(notesOut, p.removeSuffix(".md") + ".html")
            target.parentFile.mkdirs()
            val article =
                "<div class=\"notes-layout\">" + sidebar(root, p) +
                    "<article class=\"notes-article\">" + md(notesDir.file(p).asFile.readText()) + "</article></div>"
            target.writeText(
                page(
                    root,
                    "${p.substringAfterLast("/").removeSuffix(".md")} · Notes · PaperMC Despawned Items",
                    "Project note: $p",
                    "NOTES",
                    true,
                    article,
                ),
            )
        }
    }
}

// The publishable site: hand pages + assets at the root, the Dokka tree under /api/.
val assembleDocsSite by tasks.registering(Copy::class) {
    group = "documentation"
    description = "Assemble the full docs site (chrome pages + boundaried Dokka under /api/) into build/docs-site"
    dependsOn("dokkaGenerate", vendorChromeAssets, renderDocsSite)
    into(layout.buildDirectory.dir("docs-site"))
    from(layout.buildDirectory.dir("docs-pages"))
    from(layout.projectDirectory.dir("docs-theme/chrome")) {
        include("main.css", "reader.js", "nav.js", "coins.js", "fox.png")
    }
    from(layout.projectDirectory.file("assets/icon.png"))
    from(layout.buildDirectory.dir("dokka/html")) { into("api") }
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
