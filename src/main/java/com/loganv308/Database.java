package com.loganv308;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.dbcp2.BasicDataSource;

public class Database {
    public static final BasicDataSource POOL = connect();

    private static BasicDataSource connect() {
        // i < 60 is just saying try for up to 60 seconds, increase to higher number for bigger timeout val. 
        for (int i = 0; i < 60; i++) {
            // Var is local variable type inference. It tells the compiler to infer the variable. This variable would be a BasicDataSource
            var pool = tryConnect();
            if (pool.isPresent()) {
                return pool.get();
            } else {
                System.out.println("Waiting 1 second then trying again...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException iex) {}
            }
        }
        System.out.println("Unable to connect, giving up.");
        System.exit(1);
        return null;
    }

    private static Optional<BasicDataSource> tryConnect() {
        System.out.println("Connecting to database...");
        BasicDataSource POOL = new BasicDataSource();
        POOL.setUsername(Objects.requireNonNull(System.getenv("PSQL_USER"), "Missing username for database"));
        POOL.setPassword(Objects.requireNonNull(System.getenv("PSQL_PASS"), "Missing password for database"));
        POOL.setDriverClassName("org.postgresql.Driver");
        POOL.setUrl(Objects.requireNonNull(System.getenv("PSQL_URL"), "Missing url for database"));
        POOL.setInitialSize(1);
        POOL.setMaxTotal(75);
        POOL.setAutoCommitOnReturn(true);
        POOL.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        POOL.setRollbackOnReturn(true);
        
        try {
            if (!POOL.getConnection().isValid(5)) {
                System.out.println("POOL.getConnection().isValid() returned false");
                return Optional.empty();
            }
        } catch (SQLException ex) {
            System.out.println("POOL.getConnection() threw an exception!");
            ex.printStackTrace();
            try {
                POOL.close();
            } catch (SQLException ex2) {}
            return Optional.empty();
        }
        System.out.println("Connected.");
        return Optional.of(POOL);
    }

    private static String insertData() {
        return "";
    }

}
