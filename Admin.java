package com.attendance;

import java.io.IOException;
import java.util.*;

public class Admin {
    private final FileManager files;

    public Admin(FileManager files) { this.files = files; }

    public void run() {
        while (true) {
            Utils.heading("ADMIN MENU");
            System.out.println("1. Add student");
            System.out.println("2. List students");
            System.out.println("3. Update student");
            System.out.println("4. Delete student");
            System.out.println("5. Add teacher");
            System.out.println("6. List teachers");
            System.out.println("7. Delete teacher");
            System.out.println("8. Add course");
            System.out.println("9. List courses");
            System.out.println("10. Delete course");
            System.out.println("11. Enroll student in course");
            System.out.println("0. Logout");
            String choice = Utils.line("Choose: ");
            try {
                switch (choice) {
                    case "1" -> addStudent();
                    case "2" -> listStudents();
                    case "3" -> updateStudent();
                    case "4" -> deleteStudent();
                    case "5" -> addTeacher();
                    case "6" -> listTeachers();
                    case "7" -> deleteTeacher();
                    case "8" -> addCourse();
                    case "9" -> listCourses();
                    case "10" -> deleteCourse();
                    case "11" -> enroll();
                    case "0" -> { return; }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception ex) {
                System.out.println("Operation failed: " + ex.getMessage());
            }
        }
    }

    private void addStudent() throws IOException {
        String username = Utils.required("Login username: ");
        ensureUsernameFree(username);
        String password = Utils.required("Login password: ");
        String roll = Utils.required("Roll number: ");
        if (findStudentByRoll(roll) != null) throw new IllegalArgumentException("Roll number already exists.");
        String name = Utils.required("Student name: ");
        String cls = Utils.required("Class: ");
        long userId = files.nextId("users.txt");
        long id = files.nextId("students.txt");
        files.append("users.txt", userId + "|" + username + "|" + FileManager.hash(password) + "|STUDENT");
        files.append("students.txt", id + "|" + userId + "|" + roll + "|" + name + "|" + cls);
        System.out.println("Student added. Student profile ID: " + id);
    }

    private void listStudents() throws IOException {
        Utils.heading("STUDENTS");
        System.out.printf("%-6s %-8s %-12s %-20s %-12s %-15s%n", "ID", "USER ID", "ROLL", "NAME", "CLASS", "USERNAME");
        Map<String, String> usernames = usernameMap();
        for (String row : files.read("students.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 5) System.out.printf("%-6s %-8s %-12s %-20s %-12s %-15s%n",
                    f[0], f[1], f[2], f[3], f[4], usernames.getOrDefault(f[1], "-"));
        }
    }

    private void updateStudent() throws IOException {
        listStudents();
        long id = Utils.id("Student profile ID to update: ");
        List<String> rows = files.read("students.txt");
        boolean found = false;
        for (int i = 0; i < rows.size(); i++) {
            String[] f = rows.get(i).split("\\|", -1);
            if (f.length == 5 && Long.parseLong(f[0]) == id) {
                f[3] = Utils.required("New name: ");
                f[4] = Utils.required("New class: ");
                rows.set(i, String.join("|", f));
                found = true;
                break;
            }
        }
        if (!found) throw new IllegalArgumentException("Student not found.");
        files.write("students.txt", rows);
        System.out.println("Student updated.");
    }

    private void deleteStudent() throws IOException {
        listStudents();
        long id = Utils.id("Student profile ID to delete: ");
        String[] student = findStudentById(id);
        if (student == null) throw new IllegalArgumentException("Student not found.");
        if (!Utils.confirm("Delete this student and their attendance?")) return;
        removeMatching("students.txt", 0, String.valueOf(id));
        removeMatching("users.txt", 0, student[1]);
        removeMatching("enrollments.txt", 0, String.valueOf(id));
        removeMatching("attendance.txt", 1, String.valueOf(id));
        System.out.println("Student deleted.");
    }

    private void addTeacher() throws IOException {
        String username = Utils.required("Login username: ");
        ensureUsernameFree(username);
        String password = Utils.required("Login password: ");
        String name = Utils.required("Teacher name: ");
        String dept = Utils.required("Department: ");
        long userId = files.nextId("users.txt");
        long id = files.nextId("teachers.txt");
        files.append("users.txt", userId + "|" + username + "|" + FileManager.hash(password) + "|TEACHER");
        files.append("teachers.txt", id + "|" + userId + "|" + name + "|" + dept);
        System.out.println("Teacher added. Teacher profile ID: " + id);
    }

    private void listTeachers() throws IOException {
        Utils.heading("TEACHERS");
        Map<String, String> usernames = usernameMap();
        System.out.printf("%-6s %-8s %-20s %-15s %-15s%n", "ID", "USER ID", "NAME", "DEPARTMENT", "USERNAME");
        for (String row : files.read("teachers.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4) System.out.printf("%-6s %-8s %-20s %-15s %-15s%n",
                    f[0], f[1], f[2], f[3], usernames.getOrDefault(f[1], "-"));
        }
    }

    private void deleteTeacher() throws IOException {
        listTeachers();
        long id = Utils.id("Teacher profile ID to delete: ");
        String[] teacher = findTeacherById(id);
        if (teacher == null) throw new IllegalArgumentException("Teacher not found.");
        if (!Utils.confirm("Delete teacher, courses and related attendance?")) return;
        Set<String> courseIds = new HashSet<>();
        for (String row : files.read("courses.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4 && f[3].equals(String.valueOf(id))) courseIds.add(f[0]);
        }
        removeMatching("teachers.txt", 0, String.valueOf(id));
        removeMatching("users.txt", 0, teacher[1]);
        for (String courseId : courseIds) deleteCourseData(courseId);
        System.out.println("Teacher deleted.");
    }

    private void addCourse() throws IOException {
        listTeachers();
        long teacherId = Utils.id("Assigned teacher profile ID: ");
        if (findTeacherById(teacherId) == null) throw new IllegalArgumentException("Teacher not found.");
        String code = Utils.required("Course code: ").toUpperCase(Locale.ROOT);
        for (String row : files.read("courses.txt")) if (row.split("\\|", -1)[1].equalsIgnoreCase(code))
            throw new IllegalArgumentException("Course code already exists.");
        String name = Utils.required("Course name: ");
        long id = files.nextId("courses.txt");
        files.append("courses.txt", id + "|" + code + "|" + name + "|" + teacherId);
        System.out.println("Course added. Course ID: " + id);
    }

    private void listCourses() throws IOException {
        Utils.heading("COURSES");
        Map<String, String> teacherNames = new HashMap<>();
        for (String row : files.read("teachers.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4) teacherNames.put(f[0], f[2]);
        }
        System.out.printf("%-6s %-10s %-24s %-20s%n", "ID", "CODE", "COURSE", "TEACHER");
        for (String row : files.read("courses.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4) System.out.printf("%-6s %-10s %-24s %-20s%n",
                    f[0], f[1], f[2], teacherNames.getOrDefault(f[3], "-"));
        }
    }

    private void deleteCourse() throws IOException {
        listCourses();
        String id = String.valueOf(Utils.id("Course ID to delete: "));
        if (!exists("courses.txt", 0, id)) throw new IllegalArgumentException("Course not found.");
        if (Utils.confirm("Delete this course and all its attendance data?")) {
            deleteCourseData(id);
            System.out.println("Course deleted.");
        }
    }

    private void enroll() throws IOException {
        listStudents();
        long studentId = Utils.id("Student profile ID: ");
        if (findStudentById(studentId) == null) throw new IllegalArgumentException("Student not found.");
        listCourses();
        long courseId = Utils.id("Course ID: ");
        if (!exists("courses.txt", 0, String.valueOf(courseId))) throw new IllegalArgumentException("Course not found.");
        for (String row : files.read("enrollments.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 2 && f[0].equals(String.valueOf(studentId)) && f[1].equals(String.valueOf(courseId)))
                throw new IllegalArgumentException("Student is already enrolled.");
        }
        files.append("enrollments.txt", studentId + "|" + courseId);
        System.out.println("Enrollment saved.");
    }

    private void ensureUsernameFree(String username) throws IOException {
        for (String row : files.read("users.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4 && f[1].equalsIgnoreCase(username)) throw new IllegalArgumentException("Username already exists.");
        }
    }

    private Map<String, String> usernameMap() throws IOException {
        Map<String, String> map = new HashMap<>();
        for (String row : files.read("users.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4) map.put(f[0], f[1]);
        }
        return map;
    }

    private String[] findStudentById(long id) throws IOException {
        for (String row : files.read("students.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 5 && f[0].equals(String.valueOf(id))) return f;
        }
        return null;
    }

    private String[] findStudentByRoll(String roll) throws IOException {
        for (String row : files.read("students.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 5 && f[2].equalsIgnoreCase(roll)) return f;
        }
        return null;
    }

    private String[] findTeacherById(long id) throws IOException {
        for (String row : files.read("teachers.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4 && f[0].equals(String.valueOf(id))) return f;
        }
        return null;
    }

    private boolean exists(String file, int field, String value) throws IOException {
        for (String row : files.read(file)) {
            String[] f = row.split("\\|", -1);
            if (f.length > field && f[field].equals(value)) return true;
        }
        return false;
    }

    private void removeMatching(String file, int field, String value) throws IOException {
        List<String> rows = files.read(file);
        rows.removeIf(row -> {
            String[] f = row.split("\\|", -1);
            return f.length > field && f[field].equals(value);
        });
        files.write(file, rows);
    }

    private void deleteCourseData(String courseId) throws IOException {
        removeMatching("courses.txt", 0, courseId);
        removeMatching("enrollments.txt", 1, courseId);
        Set<String> sessionIds = new HashSet<>();
        for (String row : files.read("sessions.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 5 && f[1].equals(courseId)) sessionIds.add(f[0]);
        }
        removeMatching("sessions.txt", 1, courseId);
        for (String sessionId : sessionIds) removeMatching("attendance.txt", 1, sessionId);
    }
}