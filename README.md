# Lost & Found (Spring Boot)

Run:
    mvn spring-boot:run

Open: http://localhost:8080

Logins
- Student demo : demo@college.com / demo123
- Dean office  : dean@college.com / dean123   (admin panel at /admin)
  Change with app.admin.email / app.admin.password in application.properties
  (or APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD environment variables on Render).

Rules
- Everyone can see all complaints, their status and Dean office updates.
- A student can edit / resolve / delete ONLY their own complaint.
- Dean office can change the status, add a note and delete any complaint.

Needs JDK 17 or 21 and Maven. Database is H2 (file ./data).
