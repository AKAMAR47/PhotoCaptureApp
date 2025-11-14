package com.example.photocapture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {

    public List<String> readNamesFromCSV(InputStream inputStream) {
        List<String> names = new ArrayList<>();
        
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                for (String value : values) {
                    String trimmedValue = value.trim();
                    if (!trimmedValue.isEmpty()) {
                        names.add(trimmedValue);
                    }
                }
            }
            
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return names;
    }
}
