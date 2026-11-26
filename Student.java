package com.attendance;

import java.io.IOException;
import java.util.*;

public class Student {
    private final FileManager files;
    private final long userId;
    private final String username;

    public Student(FileManager files, long userId, String username) {
        this.files = files;
        this.userId = userId;
        this.username = username;
    }

    public void run() {
        while (true) {
            Utils.heading("STUDENT MENU - " + username);
            System.out.println("1. My attendance");
            System.out.println("0. Logout");
            String choice = Utils.line("Choose: ");
            try {
                if (choice.equals("0")) return;
                if (choice.equals("1")) showAttendance();
                else System.out.println("Invalid choice.");
            } catch (Exception ex) {
                System.out.println("Could not load attendance: " + ex.getMessage());
            }
        }
    }

    private void showAttendance() throws IOException {
        String studentId = null;
        for (String row : files.read("students.txt")) {
            String[] s = row.split("\\|", -1);
            if (s.length == 5 && s[1].equals(String.valueOf(userId))) studentId = s[0];
        }
        if (studentId == null) {
            System.out.println("Student profile not found.");
            return;
        }
        Utils.heading("MY COURSE ATTENDANCE");
        System.out.printf("%-10s %-24s %8s %8s %8s %8s %10s%n",
                "CODE", "COURSE", "PRESENT", "LATE", "ABSENT", "TOTAL", "PERCENT");
        boolean found = false;
        for (String enrollment : files.read("enrollments.txt")) {
            String[] e = enrollment.split("\\|", -1);
            if (e.length != 2 || !e[0].equals(studentId)) continue;
            String[] course = findCourse(e[1]);
            if (course == null) continue;
            int p = 0, l = 0, a = 0;
            Set<String> courseSessions = new HashSet<>();
            for (String session : files.read("sessions.txt")) {
                String[] s = session.split("\\|", -1);
                if (s.length == 5 && s[1].equals(e[1])) courseSessions.add(s[0]);
            }
            for (String record : files.read("attendance.txt")) {
                String[] r = record.split("\\|", -1);
                if (r.length == 4 && r[1].equals(studentId) && courseSessions.contains(r[0])) {
                    switch (r[2]) { case "P" -> p++; case "L" -> l++; case "A" -> a++; }
                }
            }
            int total = p + l + a;
            System.out.printf("%-10s %-24s %8d %8d %8d %8d %9s%n",
                    course[1], course[2], p, l, a, total, Utils.percent(p, l, total));
            found = true;
        }
        if (!found) System.out.println("No course enrollments found.");
    }

    private String[] findCourse(String id) throws IOException {
        for (String row : files.read("courses.txt")) {
            String[] c = row.split("\\|", -1);
            if (c.length == 4 && c[0].equals(id)) return c;
        }
        return null;
    }
}