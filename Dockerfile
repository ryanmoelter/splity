# splity is built natively with Gradle (`./gradlew installDist`) and the resulting
# distribution is copied in here. We deliberately do NOT run Gradle inside the Docker
# build — building once on the host/CI (with the gradle cache) keeps builds fast and
# avoids Gradle-in-Docker caching issues.
#
# The installDist output is pure JVM bytecode + a POSIX shell launcher, so it is
# architecture-independent: the same artifact is copied onto each arch's base image,
# which lets buildx produce the amd64 + arm64 manifest with no QEMU emulation.
#
# Zulu JRE 17 (headless) matches the JDK the project builds with (jvmToolchain(17),
# .sdkmanrc, CI).
FROM azul/zulu-openjdk:25.0.5-jre-headless@sha256:76d3ecfc1722b1924871c26373a113c25fc4445e4e6456b624dc91f1caac1994

COPY build/install/splity /opt/splity

# config.yaml and incrementalSyncCache.db are read/written relative to the working
# directory. Mount a host directory here at runtime: `-v /host/path:/data`.
WORKDIR /data

ENTRYPOINT ["/opt/splity/bin/splity"]
