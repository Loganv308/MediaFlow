package com.loganv308;

import java.time.LocalDate;

public class utils {
    // Retrieves OS of whatever host this program is running on. 
    public String getOS() {
        return System.getProperty("os.name").toLowerCase();
    }
    
    // Retrieves todays date
    public LocalDate getDate() {
        LocalDate today = LocalDate.now();

        return today;
    }
}
