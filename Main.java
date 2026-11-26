package com.attendance;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            FileManager files = new FileManager();
            files.initialize();
            new Login(files).run();
        } catch (IOException ex) {
            System.out.println("Unable to start application: " + ex.getMessage());
        } finally {
            System.out.println("\nGoodbye!");
        }
    }
}