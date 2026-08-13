````markdown
# CRS Microservices

Repository thực hành môn **Kiến trúc Microservices**.

---

## Tổng quan hệ thống

Hệ thống CRS (Course Registration System) được phát triển theo kiến trúc Microservices, gồm các service:

| Service | Port | Vai trò |
|---------|------|---------|
| auth-service | 8081 | Xác thực người dùng và phát hành JWT |
| course-service | 8082 | Quản lý môn học và phân quyền ADMIN |
| registration-service | 8083 | Đăng ký môn học và giao tiếp với course-service |
| api-gateway | 8080 | Cổng vào duy nhất của hệ thống |

### Công nghệ

- Java 17
- Spring Boot 4.1.0
- Spring Data JPA
- Spring Security
- Spring Cloud Gateway
- JWT (JJWT 0.12.6)
- MySQL
- Maven
- Postman

---

# Lab 01 - Course Service Skeleton

## Nội dung

- Khởi tạo project Spring Boot bằng Spring Initializr.
- Cấu hình Maven.
- Kết nối MySQL.
- Tạo Entity `Course`.
- Tạo Controller mock.
- Kiểm thử API bằng Postman.
- Push source code lên GitHub.

## Service

| Service | Port | Database |
|---------|------|----------|
| course-service | 8082 | course_db |

---

# Lab 02 - Course Service CRUD

## Nội dung

Hoàn thiện chức năng CRUD cho `course-service` theo mô hình 3 lớp.

## Chức năng

- CourseRepository
- CourseDTO + Validation
- CourseService
- CourseController CRUD
- GlobalExceptionHandler
- Kết nối MySQL bằng Spring Data JPA
- Kiểm thử API bằng Postman

## API

| Method | Endpoint | Mô tả |
|--------|----------|------|
| GET | `/courses` | Lấy danh sách môn học |
| GET | `/courses/{id}` | Lấy môn học theo ID |
| POST | `/courses` | Thêm môn học |
| PUT | `/courses/{id}` | Cập nhật môn học |
| DELETE | `/courses/{id}` | Xóa môn học |

---

# Lab 03 - Registration Service & Communication Between Services

## Nội dung

Phát triển `registration-service` và thực hiện giao tiếp giữa các microservice.

## Chức năng

### Course Service

- Tìm kiếm môn học.
- Phân trang danh sách môn học.
- API nội bộ giữ chỗ.
- API nội bộ trả lại chỗ.
- Quản lý số chỗ còn lại của môn học.

### Registration Service

- Tạo đăng ký môn học.
- Lấy danh sách đăng ký.
- Hủy đăng ký.
- Kiểm tra sinh viên đã đăng ký môn học hay chưa.
- Kiểm tra môn học có tồn tại hay không.
- Kiểm tra môn học còn chỗ hay không.
- Gọi `course-service` thông qua REST API.
- Xử lý lỗi khi `course-service` không khả dụng.
- Global Exception Handler trả về JSON lỗi.

## Kiến trúc

```text
                         ┌─────────────────────┐
                         │       Postman       │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │ registration-service│
                         │       :8083         │
                         └──────────┬──────────┘
                                    │
                              REST API Call
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │    course-service   │
                         │       :8082         │
                         └──────────┬──────────┘
                                    │
                 ┌──────────────────┴──────────────────┐
                 ▼                                     ▼
        ┌─────────────────┐                   ┌─────────────────┐
        │ registration_db │                   │    course_db    │
        └─────────────────┘                   └─────────────────┘
````

---

# Lab 04 - Auth Service, JWT, API Gateway & RBAC

## Nội dung

Bổ sung cơ chế xác thực và phân quyền cho toàn hệ thống:

* Khởi tạo `auth-service`.
* Đăng nhập bằng username/password.
* Mã hóa mật khẩu bằng BCrypt.
* Sinh và ký JWT.
* Khởi tạo `api-gateway` bằng Spring Cloud Gateway Reactive.
* Định tuyến request qua Gateway.
* Gateway kiểm tra sớm Header `Authorization`.
* Xác thực JWT độc lập tại `course-service` và `registration-service`.
* Phân quyền `ADMIN` / `STUDENT`.
* API đối tác sử dụng `X-API-KEY`.
* Cấu hình CORS tại Gateway.
* Toàn bộ test bên ngoài đi qua `localhost:8080`.

## Auth Service

### Thông tin

| Service      | Port | Database |
| ------------ | ---- | -------- |
| auth-service | 8081 | auth_db  |

### Chức năng

* Đăng nhập tài khoản.
* Tìm user theo username.
* Kiểm tra password bằng BCrypt.
* Sinh JWT chứa:

  * username
  * role
  * issued time
  * expiration time
* Seed tài khoản mẫu.

### Tài khoản mẫu

| Username   | Password     | Role    |
| ---------- | ------------ | ------- |
| `admin`    | `admin123`   | ADMIN   |
| `student1` | `student123` | STUDENT |

### API

| Method | Endpoint      | Mô tả                 |
| ------ | ------------- | --------------------- |
| POST   | `/auth/login` | Đăng nhập và nhận JWT |

---

## API Gateway

### Thông tin

| Service     | Port |
| ----------- | ---- |
| api-gateway | 8080 |

Gateway là điểm vào duy nhất của hệ thống.

### Routing

| Gateway Endpoint        | Service phía sau     | Endpoint nội bộ     |
| ----------------------- | -------------------- | ------------------- |
| `/api/auth/**`          | auth-service         | `/auth/**`          |
| `/api/courses`          | course-service       | `/courses`          |
| `/api/courses/**`       | course-service       | `/courses/**`       |
| `/api/registrations`    | registration-service | `/registrations`    |
| `/api/registrations/**` | registration-service | `/registrations/**` |
| `/api/public/courses`   | course-service       | `/courses`          |

### Gateway Filter

#### AuthHeaderFilter

* `/api/auth/login` không yêu cầu JWT.
* `/api/public/courses` không yêu cầu JWT.
* `GET /api/courses/**` là public.
* Các request cần xác thực nhưng thiếu `Authorization` sẽ bị trả `401 Unauthorized`.
* Gateway chỉ kiểm tra sự tồn tại của header, không thay thế việc service tự verify JWT.

#### ApiKeyFilter

Route:

```text
/api/public/courses
```

Yêu cầu:

```text
X-API-KEY: crs-partner-key-2026
```

API Key sai hoặc thiếu:

```text
403 Forbidden
```

### CORS

Gateway được cấu hình CORS cho frontend:

```text
http://localhost:5173
```

---

# JWT Authentication

## Secret dùng chung

Các service sử dụng cùng một JWT secret:

```text
CRS-Microservices-Secret-Key-Nam-3-Hoc-Ky-2026-Doi-Trong-Thuc-Te
```

Được cấu hình tại:

* `auth-service`
* `course-service`
* `registration-service`

## JWT Flow

```text
                        ┌───────────────────┐
                        │     Client        │
                        └─────────┬─────────┘
                                  │
                           POST /api/auth/login
                                  │
                                  ▼
                        ┌───────────────────┐
                        │   API Gateway     │
                        │       :8080       │
                        └─────────┬─────────┘
                                  │
                                  ▼
                        ┌───────────────────┐
                        │   auth-service    │
                        │       :8081       │
                        └─────────┬─────────┘
                                  │
                           Username/Password
                                  │
                                  ▼
                              BCrypt
                                  │
                                  ▼
                                JWT
                                  │
                                  ▼
                        ┌───────────────────┐
                        │      Client       │
                        └─────────┬─────────┘
                                  │
                       Authorization: Bearer JWT
                                  │
                                  ▼
                        ┌───────────────────┐
                        │   API Gateway     │
                        │       :8080       │
                        └─────────┬─────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
          ┌──────────────────┐        ┌────────────────────┐
          │  course-service  │        │ registration-service│
          │      :8082       │        │        :8083       │
          └──────────────────┘        └────────────────────┘
                    │                           │
                    │                           │
              tự verify JWT               tự verify JWT
```

---

# RBAC - Phân quyền

## Course Service

| Method | Endpoint       | Quyền    |
| ------ | -------------- | -------- |
| GET    | `/courses/**`  | Public   |
| POST   | `/courses/**`  | ADMIN    |
| PUT    | `/courses/**`  | ADMIN    |
| DELETE | `/courses/**`  | ADMIN    |
| ALL    | `/internal/**` | Internal |

## Registration Service

| Endpoint            | Quyền              |
| ------------------- | ------------------ |
| `/registrations/**` | ADMIN hoặc STUDENT |

`registration-service` chỉ yêu cầu người dùng đã đăng nhập, không phân biệt role.

---

# Kiểm thử qua API Gateway

Từ Lab 04, các request kiểm thử từ bên ngoài sử dụng:

```text
http://localhost:8080
```

## Bộ test

| STT | Test                                            | Kết quả mong đợi   |
| --: | ----------------------------------------------- | ------------------ |
|   1 | Login ADMIN                                     | `200 OK` + JWT     |
|   2 | Login STUDENT                                   | `200 OK` + JWT     |
|   3 | GET `/api/courses` không token                  | `200 OK`           |
|   4 | POST `/api/courses` không token                 | `401 Unauthorized` |
|   5 | POST `/api/courses` bằng STUDENT                | `403 Forbidden`    |
|   6 | POST `/api/courses` bằng ADMIN                  | `201 Created`      |
|   7 | POST `/api/registrations` bằng STUDENT          | `201 Created`      |
|   8 | GET `/api/public/courses` với API Key đúng      | `200 OK`           |
|   9 | GET `/api/public/courses` với API Key sai/thiếu | `403 Forbidden`    |

## Hai case quan trọng

### Gateway chặn request thiếu Authorization

```text
POST /api/courses
        │
        ▼
API Gateway
        │
Không có Authorization
        │
        ▼
401 Unauthorized
```

### Gateway cho qua nhưng Service từ chối do role

```text
POST /api/courses
Authorization: Bearer <STUDENT_TOKEN>
        │
        ▼
API Gateway
        │
Có Authorization
        │
        ▼
course-service
        │
JWT hợp lệ
role = STUDENT
        │
POST yêu cầu ADMIN
        │
        ▼
403 Forbidden
```

Hai trường hợp này chứng minh Gateway và từng service có hai lớp bảo vệ độc lập.

---

# Cấu trúc Repository

```text
crs-microservices/
│
├── auth-service/
│   ├── src/
│   └── pom.xml
│
├── course-service/
│   ├── src/
│   └── pom.xml
│
├── registration-service/
│   ├── src/
│   └── pom.xml
│
└── api-gateway/
    ├── src/
    └── pom.xml
```

---

# Môi trường phát triển

* Java 17
* Spring Boot 4.1.0
* Spring Cloud Gateway
* Maven
* MySQL
* Postman
* Git / GitHub

---

# Git

Các phần chính của Lab 04 được tách thành các commit:

```bash
git add auth-service/
git commit -m "init: auth-service with JWT login + seed data"

git add api-gateway/
git commit -m "init: api-gateway routing + auth header filter + api key filter"

git add course-service/ registration-service/
git commit -m "feat: jwt verification + role-based authorization across services"
```

```
```
