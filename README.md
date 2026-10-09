# Campus Mate

## Project Overview

Campus Mate is a centralized college and student portal built as an academic project. It brings common campus information and student tools together in one web application, so students can view college updates and organize academic tasks, while administrators maintain shared portal content.

## Main Features

- Student-only public registration, login, and logout; Faculty and Admin accounts are created and managed by Admin
- Student dashboard with recent notices, upcoming events, the current day's timetable, and the signed-in student's pending tasks
- Admin dashboard with portal summary counts
- Notices and events
- Timetable viewing and administration
- Academic Planner for creating, editing, completing, and deleting personal tasks
- Clubs and learning resources
- Student profile viewing and editing
- Search across notices, events, clubs, and resources
- Role-based access for students, faculty, and administrators
- Faculty login flow with teacher/faculty role selection and subject/class access checks
- Subject catalog and teacher-subject-semester-section assignment management
- Academic Calendar with admin-managed academic dates and read-only student/faculty viewing
- Teacher Notes / Resources: faculty upload class materials and students view/download materials for their class
- Server-side input validation on forms
- Ownership checks that restrict task operations and profile updates to the signed-in student

## Technology Stack

- Java 17
- Spring Boot 4.0.8
- Spring MVC (Spring Boot Web MVC)
- Spring Data JPA and Hibernate
- Maven
- MySQL with the MySQL Connector/J driver
- Thymeleaf templates, HTML, and CSS
- Spring Security with BCrypt password encoding

The current project does not contain a JavaScript dependency or standalone JavaScript files.

## User Roles

- **STUDENT** — Can access the student dashboard and campus modules, view and edit their own profile, manage their own planner tasks.
- **FACULTY / TEACHER** — Can sign in through the teacher/faculty login option, access the faculty dashboard, view their own assigned subjects/classes, and upload or delete their own notes for those assigned classes.
- **ADMIN** — Can access the admin dashboard and administration pages for notices, events, timetable entries, clubs, and resources. Admin accounts are created or reconciled by the configured admin account seeder.

Admin pages are restricted to the `ADMIN` role. Faculty pages are restricted to the `FACULTY` role. Profile and planner routes are restricted to the `STUDENT` role. The Academic Calendar is available read-only to authenticated students and faculty; only admins can manage calendar entries. Other authenticated portal pages are available to authenticated users. Admin continues to manage URL-based resources; Step 20 teacher file uploads are available only to Faculty.

## Database

Campus Mate uses MySQL. The configured database name is `campus_mate`.

Spring Data JPA and Hibernate manage the schema. The current setting is `spring.jpa.hibernate.ddl-auto=update`, which updates the schema as the application starts. It does not replace the need to keep database backups.

Configure your own local database user and password. Do not put real credentials in this README or commit or share local credential files.

## Configuration

The main settings are in `src/main/resources/application.properties`. It optionally imports `application-local.properties` from the project root. That local file is ignored by Git and can hold machine-specific settings.

The application also supports environment-variable overrides for the database connection. A local configuration can use values like:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/campus_mate
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

Alternatively, set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the environment. The default URL in the main configuration points to `localhost:3306/campus_mate`; the username and password default to empty values. The application expects the admin seed email and password properties (`campusmate.admin.email` and `campusmate.admin.password`) to be configured for the admin account seeder. Set them to credentials you control in your local configuration, and keep that configuration private.

## How to Run

1. Install Java 17 or a compatible JDK and confirm `java -version` works.
2. Install Maven and confirm `mvn -version` works.
3. Install and start MySQL.
4. Create the `campus_mate` database in MySQL if it does not already exist. For example, run `CREATE DATABASE campus_mate;` using your MySQL client.
5. Configure your database URL, username, and password using `application-local.properties` or the environment variables described above. Configure the admin seed properties locally as well.
6. From the project root, run the application with Maven:

   ```bash
   mvn spring-boot:run
   ```

7. Open `http://localhost:8080` in a browser. No custom server port is set in the project configuration, so Spring Boot uses its default port, 8080.

## Project Structure

```text
src/
├── main/
│   ├── java/com/campusmate/
│   │   ├── CampusMateApplication.java
│   │   ├── AuthController.java
│   │   ├── HomeController.java
│   │   ├── config/       # Security configuration and admin account seeder
│   │   ├── controller/   # Admin, club, profile, resource, and search controllers
│   │   ├── model/        # JPA entities and form/view models
│   │   ├── repository/   # Spring Data repositories
│   │   └── service/      # Application services
│   └── resources/
│       ├── application.properties
│       ├── static/css/style.css
│       └── templates/    # Thymeleaf pages, including admin pages under admin/
└── test/
    ├── java/com/campusmate/controller/
    ├── java/com/campusmate/service/
    └── resources/application.properties
```

The Academic Calendar uses `AcademicCalendarEntry` and `AcademicCalendarEventType` in `model`, `AcademicCalendarRepository` in `repository`, and `AcademicCalendarService` with its controller in their corresponding packages. Its student/faculty view is `templates/calendar.html`; admin list and form pages are under `templates/admin/`.

## Important Routes

These are routes mapped by the current application. Form submissions use the listed methods where applicable.

| Area | Routes |
| --- | --- |
| Home and authentication | `GET /`, `GET /register`, `POST /register`, `GET /login`, `POST /login` (Spring Security form processing), `POST /logout` |
| Student dashboard and campus modules | `GET /dashboard`, `GET /notices`, `GET /events`, `GET /timetable`, `GET /clubs`, `GET /resources` |
| Academic Planner | `GET /planner`, `GET /planner/new`, `POST /planner`, `GET /planner/edit/{id}`, `POST /planner/update/{id}`, `POST /planner/delete/{id}`, `POST /planner/complete/{id}` |
| Academic Calendar | `GET /calendar` (authenticated student, faculty, or admin; read-only view) |
| Student profile | `GET /profile`, `POST /profile` |
| Student resources | `GET /resources`; `GET /resources/{id}/download` for class-authorized teacher files |
| Search | `GET /search` (optional `query` parameter) |
| Admin dashboard | `GET /admin`, `GET /admin/`, `GET /admin/dashboard` redirect to `GET /dashboard/admin`; `GET /dashboard/admin` displays the dashboard |
| Admin content management | `/admin/notices`, `/admin/events`, `/admin/timetable`, `/admin/clubs`, and `/admin/resources` (with the corresponding mapped create, edit, update, and delete routes) |
| Admin calendar management | `GET /admin/calendar`, `GET /admin/calendar/new`, `POST /admin/calendar`, `GET /admin/calendar/edit/{id}`, `POST /admin/calendar/update/{id}`, `POST /admin/calendar/delete/{id}` |
| Faculty notes | `GET /faculty/resources`, `GET /faculty/resources/new`, `POST /faculty/resources`, `POST /faculty/resources/{id}/delete` |

## Security

- Student passwords are encoded using BCrypt through Spring Security's `PasswordEncoder`.
- Authorization is role-based. Admin routes require `ADMIN`; profile and planner routes require `STUDENT`.
- Planner service operations load tasks for the authenticated student and protect task access by ownership.
- Profile reads and updates use the authenticated user's account, protecting profile ownership.
- Password values are not exposed through the profile view model; profile editing covers the profile fields implemented by the application.
- Resource URLs are validated server-side to accept HTTP or HTTPS URLs.
- Registration, profile, planner, and admin content forms use server-side validation where their form models specify constraints.
- Logout invalidates the HTTP session and removes the session cookie.
- Academic calendar views require an authenticated Student, Faculty, or Admin role; `/admin/calendar` management routes require `ADMIN` in the security filter and controller authorization.
- Calendar validation rejects blank or overlong required fields and any end date before the start date.

## Step 16: Faculty Role and Teaching Assignment Flow

Step 16 adds the faculty/teacher layer without altering the existing student or admin workflow.

### Included in Step 16

- `FACULTY` role support in user accounts, login flow, and security restrictions
- Teacher/faculty login option on the login page, with validation that selected login type matches the authenticated role
- Subject catalog management for course codes, department, and semester association
- Teacher-subject-semester-section assignment records used to authorize faculty access
- Faculty dashboard with read-only access to assigned subjects, semesters, and sections
- Admin screens for managing faculty records, subjects, and teacher-subject assignments

### Step 16 Routes

- `GET /faculty`, `GET /faculty/`, `GET /faculty/dashboard`
- `GET /admin/faculty`, `POST /admin/faculty`
- `GET /admin/subjects`, `POST /admin/subjects`
- `GET /admin/teacher-subject-assignments`, `POST /admin/teacher-subject-assignments`

### Security Notes for Step 16

- Faculty-only pages require the `FACULTY` authority. Faculty can view teaching assignments linked to their own account; Admin manages those records.
- Admin-only management screens retain the `ADMIN` restriction.

## Step 17: Academic Calendar

The Academic Calendar is an information-only module. It does not provide attendance functionality.

- Admins manage entries at `/admin/calendar`: list, add, edit, and delete academic dates.
- Students, faculty, and admins can view the shared calendar at `/calendar`. Students and faculty have read-only access; backend authorization rejects their attempts to use admin management routes.
- Entries are ordered by start date ascending, then title and ID for stable ordering. Dates display in a user-friendly day/month/year format.
- A calendar entry contains a title, description, event type, required start date, optional end date, and automatic created/updated timestamps.
- Supported event types are Semester Start, Semester End, Examination, Holiday, College Event, and Important Academic Date.
- Calendar validation requires a non-blank title and description, a start date and event type. Title is limited to 160 characters, description to 2,000 characters, and an optional end date cannot precede the start date.
- Hibernate/JPA manages the new `academic_calendar_entries` table using the existing `ddl-auto=update` configuration. Existing database data is not reset.

## Step 20: Teacher Notes and Resource Files

- Admin continues to create/manage Faculty accounts and assign Faculty to Subject + Department + Semester + Section through the existing Step 16 pages. Admin does not upload or manage teacher notes.
- Faculty uploads notes only for an existing assignment owned by their account, can view their own uploads, and can delete only their own files. Assignment ownership and resource ownership are checked on the server.
- Students see the existing shared URL resources plus uploaded files matching their account's Department, Semester, and Section. A download request repeats this authorization check against the resource ID.
- Single-file uploads support PDF, DOC/DOCX, PPT/PPTX, XLS/XLSX, and TXT. The default maximum is 10 MB. The original filename is display metadata only; a generated safe filename is stored under the configured upload directory.
- Set `app.upload.dir` to a writable local directory (default `uploads/resources`) and `app.upload.max-file-size` to a byte count (default `10485760`). The upload directory is created when needed. Test configuration points to a directory under the system temporary path.
- Step 20 is a notes/resource file module only. It does not restore student homework/assignments or add attendance or notification features.

## Testing

The Step 16 verification set covers faculty login, Faculty Management, Subject Management, and teacher-subject-class assignment access.

The full suite includes Step 16 faculty login and teaching assignment tests, Step 17 calendar CRUD and role checks, and Step 20 resource upload, class access, and download coverage.

Current verified results:

- `mvn clean test` — passed; 34 tests, 0 failures, 0 errors, 0 skipped.
- `mvn clean compile` — passed.
- `git diff --check` — passed.

No browser-test result is claimed here.

## Development Notes

- Hibernate is configured with `ddl-auto=update`.
- Keep existing database data; do not delete or reset the database as part of routine development.
- Keep local credentials private and out of Git commits and shared documentation.
- The application uses Spring MVC, services, repositories, Thymeleaf, and JPA in a lightweight architecture without unnecessary heavy infrastructure.

## Future Scope

Possible future improvements, not currently implemented, could include automated deployment, additional accessibility refinements, and expanded reporting for administrators.
