package com.loganv308;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SpaceSavingCalculator {
    // Class attribute(s)
    Double spaceSaveValue;
    
    // Default Constructor, requires a value to be passed in.
    SpaceSavingCalculator(Double spaceSaveValue){
        this.spaceSaveValue = 0.0;
    }

    // ----------------- Getter -----------------
    public Double getSpaceSaveValue() {
        return spaceSaveValue;
    }

    // ----------------- Setter -----------------
    public void setSpaceSaveValue(Double spaceSaveValue) {
        this.spaceSaveValue = spaceSaveValue;
    }

    // Grabs the expected file size and transforms it to GB
    public Double getExpectedFileSize(Path nasPath) {
        try {
            long bytes = Files.size(nasPath);
            
            spaceSaveValue += bytes / (1024.0 * 1024.0 * 1024.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Math.floor(spaceSaveValue * 100) / 100;
    }

    // Calculates total space saved.
    public Double spaceSaved(Double initialVal, Double localVal) {
        spaceSaveValue = initialVal - localVal;
        
        return spaceSaveValue;
    }

    // Stores the value within TotalSaved.json file. 
    public void storeValue(Double fileSize, SpaceSavingCalculator val) {
        ObjectMapper om = new ObjectMapper();

        // SerializationFeature is used for pretty printing
        om.enable(SerializationFeature.INDENT_OUTPUT);

        // Try creating the new file, displays IOException error if it doesn't work. 
        try {
            File file = new File("TotalSaved.json");
            
            om.writeValue(file, val);

            System.out.println("JSON file created, total storage saved.");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "SpaceSavingCalculator [spaceSaveValue=" + spaceSaveValue + "]";
    }
    
}
