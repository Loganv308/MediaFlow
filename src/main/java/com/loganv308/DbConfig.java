package com.loganv308;

final class DbConfig {
    final String host;
    final int port;
    final String name;
    final String user;
    final String password;

    DbConfig(String host, int port, String name, String user, String password) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.user = user;
        this.password = password;
    }
}
