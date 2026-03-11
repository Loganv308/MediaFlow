package com.loganv308;

import java.time.LocalDate;

public class utils {
    public String getOS() {
        return System.getProperty("os.name").toLowerCase();
    }
    
    public LocalDate getDate() {
        LocalDate today = LocalDate.now();

        return today;
    }
}
