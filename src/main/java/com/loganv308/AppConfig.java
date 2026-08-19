package com.loganv308;

final class AppConfig {
    final PathConfig pathConfig;
    final DbConfig dbConfig;
    final long scanIntervalMillis;

    AppConfig(PathConfig pathConfig, DbConfig dbConfig, long scanIntervalMillis) {
        this.pathConfig = pathConfig;
        this.dbConfig = dbConfig;
        this.scanIntervalMillis = scanIntervalMillis;
    }
}
