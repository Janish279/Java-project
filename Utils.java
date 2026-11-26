package com.attendance;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class Utils {
    public static final Scanner SCANNER = new Scanner(System.in);
    private Utils() { }

    public static String line(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    public static String required(String prompt) {
        while (true) {
            String value = line(prompt);
            if (value.isBlank()) {
                System.out.println("This field cannot be empty.");
            } else if (value.contains("|") || value.contains("\n") || value.contains("\r")) {
                System.out.println("Please do not use the | character or a line break.");
            } else {
                return value;
            }
        }
    }

    public static int number(String prompt, int min, int max) {
        while (true) {
            String value = line(prompt);
            try {
                int n = Integer.parseInt(value);
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException ignored) { }
            System.out.printf("Enter a number from %d to %d.%n", min, max);
        }
    }

    public static long id(String prompt) {
        while (true) {
            String value = line(prompt);
            try {
                long id = Long.parseLong(value);
                if (id > 0) return id;
            } catch (NumberFormatException ignored) { }
            System.out.println("Enter a valid positive ID.");
        }
    }

    public static LocalDate date(String prompt) {
        while (true) {
            String value = required(prompt);
            try { return LocalDate.parse(value); }
            catch (DateTimeParseException ex) { System.out.println("Use YYYY-MM-DD, for example 2025-01-15."); }
        }
    }

    public static boolean confirm(String prompt) {
        String answer = line(prompt + " (y/N): ");
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    public static void heading(String title) {
        System.out.println("\n========================================");
        System.out.println(" " + title);
        System.out.println("========================================");
    }

    public static String percent(int present, int late, int total) {
        if (total == 0) return "N/A";
        return String.format("%.2f%%", ((present + 0.5 * late) / total) * 100.0);
    }
}