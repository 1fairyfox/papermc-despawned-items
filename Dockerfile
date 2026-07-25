# Reproducible Linux build/test environment for papermc-despawned-items.
#
# Local-first, per the fairyfox `docker` standard (notes/reference/docker.md): the
# Linux build/test loop runs HERE on the Windows dev box in a container; CI is the
# backstop gate, not where we discover whether the Linux build/tests pass.
#
# The image itself is deliberately thin — JDK 21 only. The project source and the
# checked-in Gradle wrapper are BIND-MOUNTED at runtime (see compose.yaml), so edits
# on the host reflect immediately and no COPY/rebuild cycle is needed. Testcontainers
# talks to the host Docker daemon over the socket mounted in compose.yaml (Java
# docker-java client), so no Docker CLI is needed inside the image.
FROM eclipse-temurin:21-jdk

# Match the plugin's toolchain (build.gradle.kts targets Java 21; foojay would provision
# it if absent, but the base image already carries it, keeping the build offline-fast).
WORKDIR /workspace

# Keep the Gradle home on a named volume (compose.yaml) so dependency + wrapper caches
# survive between runs and the second build is fast.
ENV GRADLE_USER_HOME=/gradle-cache

# Default: the full gate the fairyfox `testing` standard requires before a ship.
CMD ["./gradlew", "build", "--no-daemon"]
