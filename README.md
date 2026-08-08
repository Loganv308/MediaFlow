# MediaFlow

**Automated media optimization for large personal media libraries.**

MediaFlow is a Java-based media management and encoding tool I built to solve a problem in my own homelab: managing a large media library with inconsistent codecs, resolutions, and file sizes.

Instead of manually identifying and optimizing media, MediaFlow scans the library, analyzes files, and uses **FFmpeg** to automatically re-encode media that doesn't meet the desired requirements.

What started as a simple idea went through multiple iterations and extensive testing against sample data before becoming the current version.

---

## Features

- 🔎 **Automated media scanning**
- 🎥 **Video analysis with FFprobe**
- ⚙️ **Automated encoding with FFmpeg**
- 💾 **Storage savings calculations**
- ⏱️ **Configurable scan intervals**
- 🗄️ **NAS / SMB storage support**
- 🐳 **Docker & Docker Compose support**
- ⚙️ **Environment-based configuration**

### Encoding

The current workflow is designed around:

- H.265 / HEVC
- 1080p video
- FFmpeg-based encoding

> ⚠️ **Important:** MediaFlow replaces the original media file with the re-encoded version. Make sure the media directory is writable and that you have backups of anything important.

---

# How It Works

```text
Media Library
      │
      ▼
 File Scanner
      │
      ▼
 Media Analysis
      │
      ▼
Needs Encoding?
   │        │
  No       Yes
   │        │
 Skip     FFmpeg
            │
            ▼
     Optimized Media
```

MediaFlow can run a full scan-and-process cycle automatically based on the configured interval.

---

# Tech Stack

| Technology | Purpose |
|---|---|
| **Java 21** | Application development |
| **Maven** | Build & dependency management |
| **FFmpeg** | Video encoding |
| **FFprobe** | Media analysis |
| **Docker** | Containerization |
| **Docker Compose** | Deployment |
| **SMB/CIFS** | NAS connectivity |

### Java Libraries

- `dotenv-java` — Environment configuration
- `org.json` — JSON processing
- `Jackson` — JSON serialization
- `PostgreSQL JDBC` — Database connectivity
- `Apache Commons DBCP2` — Connection pooling

---

# Setup

## Requirements

- Java 21+
- Maven
- FFmpeg / FFprobe
- Docker & Docker Compose *(optional)*
- Access to your media storage

## Clone & Build

```bash
git clone https://github.com/Loganv308/MediaFlow.git
cd MediaFlow

mvn clean package
```

---

# Configuration

Copy `.env.example` to `.env` and update the values for your environment.

```env
# Local / JAR configuration
NAS_ROOT=NAS-Root
TEMP_DIR=C:\Temp\nas
FFMPEG_BIN=.\ffmpeg\ffmpeg.exe
FFPROBE_BIN=.\ffmpeg\ffprobe.exe

# Docker / NAS configuration
NAS_HOST=NAS-IP
NAS_SHARE=MediaPath
NAS_USER=nasuser
NAS_PASS=changeme

# Scan interval in minutes
SCAN_INTERVAL_MINUTES=10
```

### Configuration

| Variable | Description |
|---|---|
| `NAS_ROOT` | Root media directory |
| `TEMP_DIR` | Temporary processing directory |
| `FFMPEG_BIN` | FFmpeg executable path |
| `FFPROBE_BIN` | FFprobe executable path |
| `NAS_HOST` | NAS IP / hostname |
| `NAS_SHARE` | SMB share |
| `NAS_USER` | NAS username |
| `NAS_PASS` | NAS password |
| `SCAN_INTERVAL_MINUTES` | Time between scans |

**Never commit your real `.env` file.**

---

# Running

### Java

```bash
java -jar target/mediaencoder-1.0-SNAPSHOT.jar
```

### Docker

```bash
docker compose up -d
```

View logs with:

```bash
docker compose logs -f
```

---

# What I Learned

MediaFlow became a great opportunity to work with several areas of software engineering:

### 🧩 Designing Around a Real Problem

Rather than building a project around a predefined assignment, I started with a problem I actually had and designed the software around solving it.

### 🎥 Working With FFmpeg

I learned how to integrate an external command-line application into a Java application, including process execution, command construction, error handling, and file management.

### 📁 Filesystem & NAS Programming

Working with a large media library introduced practical challenges involving recursive file scanning, large files, network storage, permissions, and Windows/Linux filesystem differences.

### 🐳 Docker

Supporting both local execution and Docker taught me how filesystem paths, environment variables, and external storage behave differently inside containers.

### 🔄 Iterative Development

The project went through many iterations and rounds of testing against sample data. Each iteration exposed new edge cases and gave me an opportunity to improve the design.

The biggest lesson was simple:

**Build → Test → Break → Learn → Improve → Repeat.**

---

# Future Ideas

Some potential improvements include:

- GPU-specific encoding profiles
- Better processing history
- Media metadata database
- Improved reporting and statistics
- Parallel processing
- Web-based management interface
- Improved failure recovery

---

# Project Status

**Functional / Active Personal Project**

MediaFlow has reached a stable version that solves the original problem it was designed to address, while leaving plenty of room for future improvements.

---

## Author

**Logan Velier**

Software Engineer | Systems & Infrastructure Enthusiast

[GitHub](https://github.com/Loganv308) · [Portfolio](https://velier.dev)

---

### Why I Built It

MediaFlow started with a simple problem in my homelab.

I needed a better way to manage my media library, so I built one.

After many iterations, experiments, and tests against sample data, that idea became a working application that I can actually use.

**That's what makes MediaFlow one of my favorite projects.**
