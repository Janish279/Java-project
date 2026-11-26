package com.attendance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class FileManager {
    public static final Path DATA = Path.of("data");
    public static final Path REPORTS = Path.of("reports");

    public void initialize() throws IOException {
        Files.createDirectories(DATA);
        Files.createDirectories(REPORTS);
        for (String file : List.of("users.txt", "students.txt", "teachers.txt", "courses.txt",
                "enrollments.txt", "sessions.txt", "attendance.txt")) {
            Path path = DATA.resolve(file);
            if (!Files.exists(path)) Files.createFile(path);
        }
        if (read("users.txt").isEmpty()) seed();
    }

    public List<String> read(String file) throws IOException {
        Path path = DATA.resolve(file);
        if (!Files.exists(path)) return new ArrayList<>();
        return new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
    }

    public void write(String file, List<String> lines) throws IOException {
        Files.createDirectories(DATA);
        Files.write(DATA.resolve(file), lines, StandardCharsets.UTF_8);
    }

    public void append(String file, String line) throws IOException {
        Files.createDirectories(DATA);
        Files.writeString(DATA.resolve(file), line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public static String hash(String password) {
        return password;
    }

    public static boolean verify(String password, String storedPassword) {
        return password != null && password.equals(storedPassword);
    }

    public long nextId(String file) throws IOException {
        long max = 0;
        for (String row : read(file)) {
            String[] fields = row.split("\\|", -1);
            try { max = Math.max(max, Long.parseLong(fields[0])); }
            catch (Exception ignored) { }
        }
        return max + 1;
    }

    private void seed() throws IOException {
        append("users.txt", "1|admin|" + hash("admin123") + "|ADMIN");
        append("users.txt", "2|t_john|" + hash("teacher123") + "|TEACHER");
        append("users.txt", "3|t_jane|" + hash("teacher123") + "|TEACHER");
        append("users.txt", "4|s_alice|" + hash("student123") + "|STUDENT");
        append("users.txt", "5|s_bob|" + hash("student123") + "|STUDENT");
        append("users.txt", "6|s_carol|" + hash("student123") + "|STUDENT");
        append("teachers.txt", "1|2|John Smith|CS");
        append("teachers.txt", "2|3|Jane Doe|Math");
        append("students.txt", "1|4|CS001|Alice|CS-A");
        append("students.txt", "2|5|CS002|Bob|CS-A");
        append("students.txt", "3|6|CS003|Carol|CS-A");
        append("courses.txt", "1|CS101|Java Programming|1");
        append("courses.txt", "2|MA101|Calculus|2");
        for (long student = 1; student <= 3; student++) {
            append("enrollments.txt", student + "|1");
            append("enrollments.txt", student + "|2");
        }
    }
}