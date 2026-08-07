# ---- Build stage: compile the app into a single runnable jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source so `docker build` only re-downloads
# them when pom.xml changes, not on every source edit.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests


# ---- Runtime stage: NVENC-capable ffmpeg + CUDA runtime (Ubuntu 24.04) ----
# jrottenberg/ffmpeg:*-nvidia ships ffmpeg/ffprobe built with NVENC support on
# top of the nvidia/cuda runtime image — plain `apt-get install ffmpeg` does
# NOT include NVENC. Requires the host to have an NVIDIA GPU, driver, and the
# NVIDIA Container Toolkit installed (see docker-compose.yml).
FROM jrottenberg/ffmpeg:8.1.2-nvidia2404

# The base image sets `ENTRYPOINT ["ffmpeg"]` — clear it, this container runs Java.
ENTRYPOINT []

RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-21-jre-headless \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/mediaencoder-1.0-SNAPSHOT.jar /app/mediaencoder.jar

# ffmpeg/ffprobe from the base image live here.
ENV FFMPEG_BIN=/usr/local/bin/ffmpeg
ENV FFPROBE_BIN=/usr/local/bin/ffprobe

# Runtime working directory: Logs/, media_cache.ser, and TotalSaved.json are all
# written relative to the CWD, so this is what docker-compose mounts as a volume
# for persistence — kept separate from /app so app files and data don't mix.
WORKDIR /data
RUN mkdir -p /data/Logs

CMD ["java", "-jar", "/app/mediaencoder.jar"]
