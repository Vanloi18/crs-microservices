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

Lab 04 - Auth Service, JWT, API Gateway & RBAC

Nội dung

Bổ sung cơ chế xác thực và phân quyền cho toàn hệ thống:

Khởi tạo auth-service.

Đăng nhập bằng username/password.

Mã hóa mật khẩu bằng BCrypt.

Sinh và ký JWT.

Khởi tạo api-gateway bằng Spring Cloud Gateway Reactive.

Định tuyến request qua Gateway.

Gateway kiểm tra sớm Header Authorization.

Xác thực JWT độc lập tại course-service và registration-service.

Phân quyền ADMIN / STUDENT.

API đối tác sử dụng X-API-KEY.

Cấu hình CORS tại Gateway.

Toàn bộ test bên ngoài đi qua localhost:8080.

Auth Service

Thông tin

Service

Port

Database

auth-service

8081

auth_db

Chức năng

Đăng nhập tài khoản.

Tìm user theo username.

Kiểm tra password bằng BCrypt.

Sinh JWT chứa:

username

role

issued time

expiration time

Seed tài khoản mẫu.

Tài khoản mẫu

Username

Password

Role

admin

admin123

ADMIN

student1

student123

STUDENT

API

Method

Endpoint

Mô tả

POST

/auth/login

Đăng nhập và nhận JWT

API Gateway

Thông tin

Service

Port

api-gateway

8080

Gateway là điểm vào duy nhất của hệ thống.

Routing

Gateway Endpoint

Service phía sau

Endpoint nội bộ

/api/auth/**

auth-service

/auth/**

/api/courses

course-service

/courses

/api/courses/**

course-service

/courses/**

/api/registrations

registration-service

/registrations

/api/registrations/**

registration-service

/registrations/**

/api/public/courses

course-service

/courses

Gateway Filter

AuthHeaderFilter

/api/auth/login không yêu cầu JWT.

/api/public/courses không yêu cầu JWT.

GET /api/courses/** là public.

Các request cần xác thực nhưng thiếu Authorization sẽ bị trả 401 Unauthorized.

Gateway chỉ kiểm tra sự tồn tại của header, không thay thế việc service tự verify JWT.

ApiKeyFilter

Route:

/api/public/courses

Yêu cầu:

X-API-KEY: crs-partner-key-2026

API Key sai hoặc thiếu:

403 Forbidden

CORS

Gateway được cấu hình CORS cho frontend:

http://localhost:5173

JWT Authentication

Secret dùng chung

Các service sử dụng cùng một JWT secret:

CRS-Microservices-Secret-Key-Nam-3-Hoc-Ky-2026-Doi-Trong-Thuc-Te

Được cấu hình tại:

auth-service

course-service

registration-service

JWT Flow

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

RBAC - Phân quyền

Course Service

Method

Endpoint

Quyền

GET

/courses/**

Public

POST

/courses/**

ADMIN

PUT

/courses/**

ADMIN

DELETE

/courses/**

ADMIN

ALL

/internal/**

Internal

Registration Service

Endpoint

Quyền

/registrations/**

ADMIN hoặc STUDENT

registration-service chỉ yêu cầu người dùng đã đăng nhập, không phân biệt role.

Kiểm thử qua API Gateway

Từ Lab 04, các request kiểm thử từ bên ngoài sử dụng:

http://localhost:8080

Bộ test

STT

Test

Kết quả mong đợi

1

Login ADMIN

200 OK + JWT

2

Login STUDENT

200 OK + JWT

3

GET /api/courses không token

200 OK

4

POST /api/courses không token

401 Unauthorized

5

POST /api/courses bằng STUDENT

403 Forbidden

6

POST /api/courses bằng ADMIN

201 Created

7

POST /api/registrations bằng STUDENT

201 Created

8

GET /api/public/courses với API Key đúng

200 OK

9

GET /api/public/courses với API Key sai/thiếu

403 Forbidden

Hai case quan trọng

Gateway chặn request thiếu Authorization

POST /api/courses
        │
        ▼
API Gateway
        │
Không có Authorization
        │
        ▼
401 Unauthorized

Gateway cho qua nhưng Service từ chối do role

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

Hai trường hợp này chứng minh Gateway và từng service có hai lớp bảo vệ độc lập.

Cấu trúc Repository

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

Môi trường phát triển

Java 17

Spring Boot 4.1.0

Spring Cloud Gateway

Maven

MySQL

Postman

Git / GitHub

Git

Các phần chính của Lab 04 được tách thành các commit:

git add auth-service/
git commit -m "init: auth-service with JWT login + seed data"

git add api-gateway/
git commit -m "init: api-gateway routing + auth header filter + api key filter"

git add course-service/ registration-service/
git commit -m "feat: jwt verification + role-based authorization across services"



Lab 05 - React TypeScript, kết nối qua Gateway & CORS

Nội dung

Khởi tạo frontend crs-frontend bằng Vite + React + TypeScript và kết nối toàn bộ frontend với backend thông qua api-gateway.

Chức năng

Khởi tạo project bằng Vite + React + TypeScript.

Sử dụng Axios để gọi API.

Cấu hình URL Gateway bằng biến môi trường.

Frontend chỉ gọi API thông qua api-gateway:8080.

Tạo các interface TypeScript khớp với DTO/Entity của backend.

Kết nối GET /api/courses từ frontend qua Gateway.

Kiểm tra và xử lý CORS tập trung tại API Gateway.

Chuẩn bị cấu trúc frontend cho các buổi tiếp theo.

Frontend

Thành phần

Cổng

Vai trò

crs-frontend

5173

Giao diện React + TypeScript

Công nghệ

Node.js

npm

Vite

React

TypeScript

Axios

React Router DOM

Cấu trúc thư mục

crs-frontend/
├── src/
│   ├── api/
│   ├── types/
│   ├── components/
│   ├── pages/
│   ├── context/
│   ├── App.tsx
│   └── main.tsx
├── .env
├── package.json
└── ...

Cấu hình Gateway

Frontend sử dụng biến môi trường:

VITE_API_BASE_URL=http://localhost:8080

Axios instance dùng chung lấy baseURL từ biến môi trường:

import.meta.env.VITE_API_BASE_URL

Không gọi trực tiếp:

http://localhost:8081
http://localhost:8082
http://localhost:8083

TypeScript Interfaces

Course

export interface Course {
  id: number;
  tenMonHoc: string;
  soTinChi: number;
  soChoToiDa: number;
  soChoConLai: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

Authentication

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  role: 'ADMIN' | 'STUDENT';
}

Registration

export interface Registration {
  id: number;
  studentId: number;
  courseId: number;
  trangThai: 'DA_DANG_KY' | 'DA_HUY';
  ngayDangKy: string;
}

export interface RegistrationRequest {
  studentId: number;
  courseId: number;
}

API Error

export interface ApiErrorResponse {
  message?: string;
  [field: string]: string | undefined;
}

API kiểm thử

Frontend gọi:

GET http://localhost:8080/api/courses

Gateway định tuyến request tới:

course-service:8082/courses

Frontend hiển thị dữ liệu môn học lấy từ backend.

CORS

CORS được cấu hình tập trung tại api-gateway cho frontend:

http://localhost:5173

Frontend không tự cấu hình CORS và không thêm CORS riêng vào các service backend.

Kiểm thử kết nối

Khởi động đầy đủ:

auth-service          :8081
course-service        :8082
registration-service  :8083
api-gateway           :8080
crs-frontend          :5173

Chạy frontend:

npm run dev

Mở:

http://localhost:5173

Kết quả mong đợi:

Frontend truy cập thành công.

Gọi GET /api/courses qua Gateway.

Hiển thị danh sách môn học.

Không có lỗi CORS trong trình duyệt.

Git

Commit của Lab 05:

git add crs-frontend/
git commit -m "init: vite react ts + connect via api-gateway"
git push

Sản phẩm đầu ra Lab 05

crs-frontend được khởi tạo bằng Vite + React + TypeScript.

Cấu trúc api/, types/, components/, pages/, context/ được chuẩn bị.

axiosClient là nơi dùng chung để gọi API.

VITE_API_BASE_URL=http://localhost:8080 được cấu hình bằng biến môi trường.

Có 4 nhóm interface TypeScript: course.ts, auth.ts, registration.ts, apiError.ts.

Frontend gọi GET /api/courses thông qua API Gateway.

CORS được xử lý tập trung tại API Gateway.

Frontend hiển thị dữ liệu môn học từ backend.

Có commit Git cho Lab 05.
