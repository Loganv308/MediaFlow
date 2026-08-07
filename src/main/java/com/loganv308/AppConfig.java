package com.loganv308;

final class AppConfig {
    final PathConfig pathConfig;
    final long scanIntervalMillis;

    AppConfig(PathConfig pathConfig, long scanIntervalMillis) {
        this.pathConfig = pathConfig;
        this.scanIntervalMillis = scanIntervalMillis;
    }
}
