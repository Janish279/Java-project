package com.attendance;

import java.io.IOException;
import java.util.*;

public class Attendance {
    private final FileManager files;

    public Attendance(FileManager files) { this.files = files; }

    public long createSession(long courseId, String date, String slot, long teacherUserId) throws IOException {
        for (String row : files.read("sessions.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 5 && f[1].equals(String.valueOf(courseId)) && f[2].equals(date) && f[3].equalsIgnoreCase(slot))
                return Long.parseLong(f[0]);
        }
        long id = files.nextId("sessions.txt");
        files.append("sessions.txt", id + "|" + courseId + "|" + date + "|" + slot + "|" + teacherUserId);
        return id;
    }

    public void mark(long sessionId, long studentId, String status) throws IOException {
        List<String> rows = files.read("attendance.txt");
        String replacement = sessionId + "|" + studentId + "|" + status + "|" + java.time.LocalDateTime.now();
        boolean updated = false;
        for (int i = 0; i < rows.size(); i++) {
            String[] f = rows.get(i).split("\\|", -1);
            if (f.length == 4 && f[0].equals(String.valueOf(sessionId)) && f[1].equals(String.valueOf(studentId))) {
                rows.set(i, replacement);
                updated = true;
                break;
            }
        }
        if (updated) files.write("attendance.txt", rows);
        else files.append("attendance.txt", replacement);
    }

    public List<String[]> sessionRecords(long sessionId) throws IOException {
        List<String[]> result = new ArrayList<>();
        for (String row : files.read("attendance.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4 && f[0].equals(String.valueOf(sessionId))) result.add(f);
        }
        return result;
    }

    public List> courseReport(long courseId) throws IOException {
        List> result = new ArrayList<>();
        List<String> courseSessions = new ArrayList<>();
        for (String row : files.read("sessions.txt")) {
            String[] f = row.split("\\|", -1);
            if (f.length == 5 && f[1].equals(String.valueOf(courseId))) courseSessions.add(f[0]);
        }
        for (String enrollment : files.read("enrollments.txt")) {
            String[] e = enrollment.split("\\|", -1);
            if (e.length != 2 || !e[1].equals(String.valueOf(courseId))) continue;
            String[] student = findById("students.txt", e[0]);
            if (student == null) continue;
            int p = 0, l = 0, a = 0;
            for (String sessionId : courseSessions) {
                for (String[] record : sessionRecords(Long.parseLong(sessionId))) {
                    if (record[1].equals(e[0])) {
                        switch (record[2]) {
                            case "P" -> p++;
                            case "L" -> l++;
                            case "A" -> a++;
                        }
                    }
                }
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", student[0]); m.put("roll", student[2]); m.put("name", student[3]);
            m.put("present", p); m.put("late", l); m.put("absent", a); m.put("total", p + l + a);
            result.add(m);
        }
        result.sort(Comparator.comparing(m -> (String) m.get("roll")));
        return result;
    }

    public String[] findById(String file, String id) throws IOException {
        for (String row : files.read(file)) {
            String[] f = row.split("\\|", -1);
            if (f.length > 1 && f[0].equals(id)) return f;
        }
        return null;
    }
}