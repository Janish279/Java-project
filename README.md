# Smart Attendance System

A Java console-based attendance management system with role-based access control (Admin, Teacher, Student) and plain-text file persistence.

## Features

- **Admin:** Add, list, update, and delete students; add, list, and delete teachers; create, list, and delete courses; enroll students in courses.
- **Teacher:** View assigned courses, create attendance sessions (date and time slot), mark attendance (`P` = Present, `A` = Absent, `L` = Late), view session records, generate course attendance reports, filter defaulters by custom percentage threshold (default 75%), and export reports to CSV.
- **Student:** View personal course-wise attendance breakdown and percentage.
- **Storage:** Automatically initializes local `.txt` files (`data/`) and CSV export directory (`reports/`) on first run.

## Project Structure

- `Main.java` — Application entry point
- `Login.java` — Authentication and role-based menu routing
- `Admin.java` — Administrative management operations
- `Teacher.java` — Session creation, attendance marking, and reporting
- `Student.java` — Student attendance view
- `Attendance.java` — Core session, marking, and report calculation logic
- `FileManager.java` — File I/O, ID generation, and default data seeding
- `Utils.java` — Console input validation and formatting helpers

## Prerequisites

- Java Development Kit (JDK) 17 or higher

## How to Compile & Run

1. Open a terminal in the repository root directory where the `.java` files are located.
2. Compile all files into the package directory:
   ```bash
   javac -d . *.java
