# CRS Microservices

Repository thực hành môn Kiến trúc Microservices.

## Lab 01 - Course Service Skeleton

### Nội dung

- Khởi tạo project Spring Boot bằng Spring Initializr.
- Cấu hình Maven.
- Kết nối MySQL.
- Tạo Entity `Course`.
- Tạo Controller mock.
- Kiểm thử API bằng Postman.
- Push source code lên GitHub.

### Service

| Service | Port | Database |
|---------|------|----------|
| course-service | 8082 | course_db |

---

## Lab 02 - Course Service CRUD

### Nội dung

Hoàn thiện chức năng CRUD cho `course-service` theo mô hình 3 lớp.

### Chức năng

- CourseRepository
- CourseDTO + Validation
- CourseService
- CourseController CRUD
- GlobalExceptionHandler
- Kết nối MySQL bằng Spring Data JPA
- Kiểm thử API bằng Postman

### API

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/courses` | Lấy danh sách môn học |
| GET | `/courses/{id}` | Lấy môn học theo ID |
| POST | `/courses` | Thêm môn học |
| PUT | `/courses/{id}` | Cập nhật môn học |
| DELETE | `/courses/{id}` | Xóa môn học |

---

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- Maven
- MySQL
- Lombok
- Hibernate
- Postman
- Git & GitHub

---

## Database

```
course_db
```

Bảng chính

```
course
```

Các trường

- id
- ten_mon_hoc
- so_tin_chi
- so_cho_toi_da
- so_cho_con_lai

---

## Project Structure

```
course-service
├── controller
├── dto
├── entity
├── exception
├── repository
├── service
└── resources
```

---

## Cách chạy

```bash
git clone https://github.com/Vanloi18/crs-microservices.git
```

```bash
cd crs-microservices/course-service
```

Cấu hình MySQL trong `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/course_db
spring.datasource.username=root
spring.datasource.password=123456
```

Chạy project

```bash
mvn spring-boot:run
```

Hoặc chạy trực tiếp lớp

```
CourseServiceApplication
```

---

## Kiểm thử API

Sử dụng Postman.

Ví dụ lấy danh sách môn học

```
GET http://localhost:8082/courses
```

---

## Author

**Lê Văn Lợi**

- HUNRE
- DH13C6
