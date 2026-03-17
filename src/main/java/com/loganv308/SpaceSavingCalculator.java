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
    
    // Constructor
    SpaceSavingCalculator(Double spaceSaveValue){
        this.spaceSaveValue = 0.0;
    }

    public Double getSpaceSaveValue() {
        return spaceSaveValue;
    }

    public void setSpaceSaveValue(Double spaceSaveValue) {
        this.spaceSaveValue = spaceSaveValue;
    }

    public Double getExpectedFileSize(Path nasPath) {
        try {
            long bytes = Files.size(nasPath);
            spaceSaveValue += bytes / (1024.0 * 1024.0 * 1024.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return spaceSaveValue;
    }

    public void storeValue(Double fileSize, SpaceSavingCalculator val) {
        ObjectMapper om = new ObjectMapper();

        // Used for pretty printing
        om.enable(SerializationFeature.INDENT_OUTPUT);

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
