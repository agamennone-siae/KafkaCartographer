package com.kafkacartographer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        System.out.println("Kafka Cartographer");
        
        String path = ".";
        if (args.length > 0) {
            path = args[0];
        }

        File rootDir = new File(path);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.err.println("Invalid directory: " + path);
            System.exit(1);
        }

        System.out.println("Scanning: " + rootDir.getAbsolutePath());
        
        KafkaScanner scanner = new KafkaScanner();
        Map<String, Object> result = scanner.scan(rootDir);

        saveReport(result, rootDir);
    }

    private static void saveReport(Map<String, Object> data, File outputDir) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File outputFile = new File(outputDir, "topic_map.json");
        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(data, writer);
            System.out.println("Report saved to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save report: " + e.getMessage());
        }
    }
}
