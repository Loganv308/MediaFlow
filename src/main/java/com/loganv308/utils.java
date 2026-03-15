package com.loganv308;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class utils {
    // Retrieves OS of whatever host this program is running on. 
    public String getOS() {
        // Gets the property os.name and converts it to lowercase. This is a system variable. 
        return System.getProperty("os.name").toLowerCase();
    }
    
    // Retrieves todays date
    public LocalDate getDate() {
        LocalDate today = LocalDate.now();

        // Returns todays date
        return today;
    }

    public String getFormattedTime() {
        // Will format time like so: "01:26:52 pm"
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss a");

        // Returns formatted time
        return LocalDateTime.now().format(dtf);
    }
}
