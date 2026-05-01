package com.project.edu.service;

import com.project.edu.model.Course;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Service
public class CsvStorageService {
    private static final Path OUTPUT_DIR = Paths.get("data");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("courses.csv");

    public Path saveCourses(List<Course> courses) {
        try {
            Files.createDirectories(OUTPUT_DIR);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    OUTPUT_FILE,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {

                writer.write("id,platform,title,category,description,certification,pricingModel,format,specialFeatures,rating,reviewCount,url");
                writer.newLine();

                for (Course course : courses) {
                    writer.write(csv(course.getId()));
                    writer.write(',');
                    writer.write(csv(course.getPlatform()));
                    writer.write(',');
                    writer.write(csv(course.getTitle()));
                    writer.write(',');
                    writer.write(csv(course.getCategory()));
                    writer.write(',');
                    writer.write(csv(course.getDescription()));
                    writer.write(',');
                    writer.write(csv(course.getCertification()));
                    writer.write(',');
                    writer.write(csv(course.getPricingModel()));
                    writer.write(',');
                    writer.write(csv(course.getFormat()));
                    writer.write(',');
                    writer.write(csv(course.getSpecialFeatures()));
                    writer.write(',');
                    writer.write(csv(course.getRating()));
                    writer.write(',');
                    writer.write(csv(course.getReviewCount()));
                    writer.write(',');
                    writer.write(csv(course.getUrl()));
                    writer.newLine();
                }
            }

            return OUTPUT_FILE.toAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save CSV file", e);
        }
    }

    public String getOutputPath() {
        return OUTPUT_FILE.toAbsolutePath().toString();
    }

    private String csv(String value) {
        String safe = value == null ? "" : value.replace("\r", " ").replace("\n", " ").trim();
        return '"' + safe.replace("\"", "\"\"") + '"';
    }
}
