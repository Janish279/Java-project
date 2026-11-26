package com.attendance;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class Teacher {
    private final FileManager files;
    private final long userId;
    private final String username;
    private final Attendance attendance;

    public Teacher(FileManager files, long userId, String username) {
        this.files = files;
        this.userId = userId;
        this.username = username;
        this.attendance = new Attendance(files);
    }

    public void run() {
        while (true) {
            Utils.heading("TEACHER MENU - " + username);
            System.out.println("1. View my courses");
            System.out.println("2. Create attendance session");
            System.out.println("3. Mark attendance");
            System.out.println("4. View session records");
            System.out.println("5. Course report");
            System.out.println("6. Defaulters list");
            System.out.println("7. Export course report to CSV");
            System.out.println("0. Logout");
            String choice = Utils.line("Choose: ");
            try {
                switch (choice) {
                    case "1" -> listCourses();
                    case "2" -> createSession();
                    case "3" -> markAttendance();
                    case "4" -> viewSession();
                    case "5" -> showReport(false);
                    case "6" -> showReport(true);
                    case "7" -> exportReport();
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception ex) {
                System.out.println("Operation failed: " + ex.getMessage());
            }
        }
    }

    private List<String[]> ownCourses() throws IOException {
        List<String[]> result = new ArrayList<>();
        String teacherId = teacherProfileId();
        for (String row : files.read("courses.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4 && f[3].equals(teacherId)) result.add(f);
        }
        return result;
    }

    private String teacherProfileId() throws IOException {
        for (String row : files.read("teachers.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4 && f[1].equals(String.valueOf(userId))) return f[0];
        }
        throw new IllegalStateException("Teacher profile not found.");
    }

    private void listCourses() throws IOException {
        Utils.heading("MY COURSES");
        List<String[]> courses = ownCourses();
        if (courses.isEmpty()) System.out.println("No courses assigned.");
        for (String[] c : courses) System.out.printf("ID: %-4s  %-10s  %s%n", c[0], c[1], c[2]);
    }

    private String[] chooseCourse() throws IOException {
        listCourses();
        long id = Utils.id("Course ID: ");
        for (String[] c : ownCourses()) if (c[0].equals(String.valueOf(id))) return c;
        throw new IllegalArgumentException("That course is not assigned to you.");
    }

    private void createSession() throws IOException {
        String[] course = chooseCourse();
        LocalDate date = Utils.date("Session date (YYYY-MM-DD): ");
        String slot = Utils.required("Slot (e.g. P1): ");
        long id = attendance.createSession(Long.parseLong(course[0]), date.toString(), slot, userId);
        System.out.println("Session ready. Session ID: " + id);
    }

    private void markAttendance() throws IOException {
        String[] course = chooseCourse();
        List<String[]> sessions = courseSessions(course[0]);
        if (sessions.isEmpty()) {
            System.out.println("No sessions found. Create a session first.");
            return;
        }
        printSessions(sessions);
        long sessionId = Utils.id("Session ID: ");
        String[] selected = null;
        for (String[] s : sessions) if (s[0].equals(String.valueOf(sessionId))) selected = s;
        if (selected == null) throw new IllegalArgumentException("Session does not belong to this course.");
        List<String[]> students = enrolledStudents(course[0]);
        if (students.isEmpty()) {
            System.out.println("No students are enrolled in this course.");
            return;
        }
        for (String[] student : students) {
            while (true) {
                String mark = Utils.required(student[2] + " - " + student[3] + " (P/A/L): ").toUpperCase(Locale.ROOT);
                if (List.of("P", "A", "L").contains(mark)) {
                    attendance.mark(sessionId, Long.parseLong(student[0]), mark);
                    break;
                }
                System.out.println("Enter P for Present, A for Absent, or L for Late.");
            }
        }
        System.out.println("Attendance saved successfully.");
    }

    private void viewSession() throws IOException {
        String[] course = chooseCourse();
        List<String[]> sessions = courseSessions(course[0]);
        printSessions(sessions);
        long sessionId = Utils.id("Session ID: ");
        boolean valid = sessions.stream().anyMatch(s -> s[0].equals(String.valueOf(sessionId)));
        if (!valid) throw new IllegalArgumentException("Session does not belong to this course.");
        Map<String, String[]> students = studentMap();
        Utils.heading("SESSION ATTENDANCE");
        System.out.printf("%-12s %-20s %-10s %-20s%n", "ROLL", "STUDENT", "STATUS", "MARKED AT");
        for (String[] r : attendance.sessionRecords(sessionId)) {
            String[] st = students.get(r[1]);
            if (st != null) System.out.printf("%-12s %-20s %-10s %-20s%n", st[2], st[3], statusName(r[2]), r[3].replace('T', ' '));
        }
    }

    private void showReport(boolean onlyDefaulters) throws IOException {
        String[] course = chooseCourse();
        double threshold = 75.0;
        if (onlyDefaulters) {
            String value = Utils.line("Defaulter threshold % (Enter for 75): ");
            if (!value.isBlank()) threshold = Double.parseDouble(value);
            if (threshold < 0 || threshold > 100) throw new IllegalArgumentException("Threshold must be 0 to 100.");
            System.out.printf("%nStudents below %.2f%%%n", threshold);
        }
        printReport(course[0], onlyDefaulters, threshold);
    }

    private void printReport(String courseId, boolean onlyDefaulters, double threshold) throws IOException {
        Utils.heading("COURSE ATTENDANCE REPORT");
        System.out.printf("%-10s %-18s %8s %8s %8s %8s %10s%n",
                "ROLL", "STUDENT", "PRESENT", "LATE", "ABSENT", "TOTAL", "PERCENT");
        int shown = 0;
        for (Map<String, Object> r : attendance.courseReport(Long.parseLong(courseId))) {
            int p = (int) r.get("present"), l = (int) r.get("late"), a = (int) r.get("absent"), t = (int) r.get("total");
            double pct = t == 0 ? -1 : (p + 0.5 * l) / t * 100.0;
            if (onlyDefaulters && (t == 0 || pct >= threshold)) continue;
            System.out.printf("%-10s %-18s %8d %8d %8d %8d %9s%n",
                    r.get("roll"), r.get("name"), p, l, a, t, Utils.percent(p, l, t));
            shown++;
        }
        if (shown == 0) System.out.println("No matching student records.");
    }

    private void exportReport() throws IOException {
        String[] course = chooseCourse();
        List<String> lines = new ArrayList<>();
        lines.add("Roll Number,Student,Present,Late,Absent,Total,Percentage");
        for (Map<String, Object> r : attendance.courseReport(Long.parseLong(course[0]))) {
            int p = (int) r.get("present"), l = (int) r.get("late"), a = (int) r.get("absent"), t = (int) r.get("total");
            lines.add(csv(r.get("roll").toString()) + "," + csv(r.get("name").toString()) + "," + p + "," + l + "," + a + "," + t + "," + Utils.percent(p, l, t));
        }
        java.nio.file.Files.createDirectories(FileManager.REPORTS);
        java.nio.file.Path path = FileManager.REPORTS.resolve("course_" + course[1] + "_report.csv");
        java.nio.file.Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("CSV exported to " + path);
    }

    private String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

    private List<String[]> courseSessions(String courseId) throws IOException {
        List<String[]> result = new ArrayList<>();
        for (String row : files.read("sessions.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 5 && f[1].equals(courseId)) result.add(f);
        }
        return result;
    }

    private void printSessions(List<String[]> sessions) {
        for (String[] s : sessions) System.out.printf("ID:%s  Date:%s  Slot:%s%n", s[0], s[2], s[3]);
    }

    private List<String[]> enrolledStudents(String courseId) throws IOException {
        List<String[]> result = new ArrayList<>();
        for (String row : files.read("enrollments.txt")) {
            String[] e = row.split("\\|", -1);
            if (e.length == 2 && e[1].equals(courseId)) {
                for (String studentRow : files.read("students.txt")) {
                    String[] s = studentRow.split("\\|", -1);
                    if (s.length == 5 && s[0].equals(e[0])) result.add(s);
                }
            }
        }
        result.sort(Comparator.comparing(s -> s[2]));
        return result;
    }

    private Map<String, String[]> studentMap() throws IOException {
        Map<String, String[]> map = new HashMap<>();
        for (String row : files.read("students.txt")) {
            String[] s = row.split("\\|", -1);
            if (s.length == 5) map.put(s[0], s);
        }
        return map;
    }

    private String statusName(String status) {
        return switch (status) { case "P" -> "PRESENT"; case "A" -> "ABSENT"; case "L" -> "LATE"; default -> status; };
    }
}