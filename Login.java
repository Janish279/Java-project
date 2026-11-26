package com.attendance;

import java.io.IOException;
import java.util.List;

public class Login {
    private final FileManager files;

    public Login(FileManager files) { this.files = files; }

    public void run() {
        while (true) {
            Utils.heading("SMART ATTENDANCE SYSTEM");
            System.out.println("1. Login");
            System.out.println("0. Exit");
            String choice = Utils.line("Choose: ");
            if (choice.equals("0")) return;
            if (!choice.equals("1")) {
                System.out.println("Invalid choice.");
                continue;
            }
            try {
                String username = Utils.required("Username: ");
                String password = Utils.required("Password: ");
                String[] user = findUser(username);
                if (user == null || !FileManager.verify(password, user[2])) {
                    System.out.println("Invalid username or password.");
                    continue;
                }
                System.out.println("\nWelcome, " + username + "!");
                switch (user[3]) {
                    case "ADMIN" -> new Admin(files).run();
                    case "TEACHER" -> new Teacher(files, Long.parseLong(user[0]), username).run();
                    case "STUDENT" -> new Student(files, Long.parseLong(user[0]), username).run();
                    default -> System.out.println("Unknown account role.");
                }
            } catch (Exception ex) {
                System.out.println("Could not complete action: " + ex.getMessage());
            }
        }
    }

    private String[] findUser(String username) throws IOException {
        List<String> users = files.read("users.txt");
        for (String row : users) {
            String[] f = row.split("\\|", -1);
            if (f.length == 4 && f[1].equalsIgnoreCase(username)) return f;
        }
        return null;
    }
}