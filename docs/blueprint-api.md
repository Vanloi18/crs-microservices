# CRS Microservices – API Blueprint

> Tài liệu API chuẩn của hệ thống CRS (Course Registration System).  
> Nguồn sự thật: code thực tế trong repository (Lab 01 → Lab 05).

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [API Gateway](#2-api-gateway)
3. [Auth API](#3-auth-api)
4. [Course API](#4-course-api)
5. [Registration API](#5-registration-api)
6. [Partner Public API](#6-partner-public-api)
7. [Internal API (Service-to-Service)](#7-internal-api-service-to-service)
8. [Authentication & Authorization](#8-authentication--authorization)
9. [Error Response](#9-error-response)
10. [Ví dụ cURL](#10-ví-dụ-curl)

---

## 1. Tổng quan

### Kiến trúc

```text
crs-frontend (React + Vite)
        │
        │  http://localhost:8080
        ▼
   API Gateway (:8080)
        │
        ├──► auth-service          (:8081)
        ├──► course-service        (:8082)
        └──► registration-service  (:8083)

registration-service
        │  (internal REST call)
        └──► course-service/internal/courses/{id}/reserve-seat
             course-service/internal/courses/{id}/release-seat
```

### Service & Port & Database

| Service              | Port | Database         | Vai trò                               |
|----------------------|------|------------------|---------------------------------------|
| api-gateway          | 8080 | —                | Entry point, routing, filter          |
| auth-service         | 8081 | course_db¹       | Đăng nhập, phát hành JWT              |
| course-service       | 8082 | course_db        | Quản lý môn học, internal seat API    |
| registration-service | 8083 | registration_db  | Đăng ký môn học                       |

> ¹ Theo `auth-service/src/main/resources/application.properties` hiện tại, `auth-service` đang kết nối vào `course_db`.  
> Đây là thiết lập trong môi trường lab; trên production nên tách thành `auth_db` riêng.

### Công nghệ

- Java 17, Spring Boot 4.1.0
- Spring Data JPA, Spring Security
- Spring Cloud Gateway (Reactive / WebFlux)
- JWT – JJWT 0.12.6 (HS256)
- MySQL, Maven

---

## 2. API Gateway

**Base URL:** `http://localhost:8080`

Client bên ngoài (frontend, Postman) **chỉ** gọi qua Gateway. Không gọi trực tiếp vào cổng 8081/8082/8083.

### Routing Table

| Gateway Endpoint        | Method     | Backend Service      | Backend Endpoint    | Ghi chú              |
|-------------------------|------------|----------------------|---------------------|----------------------|
| `/api/auth/**`          | ANY        | auth-service :8081   | `/auth/**`          | Rewrite path         |
| `/api/courses`          | ANY        | course-service :8082 | `/courses`          | Rewrite path         |
| `/api/courses/**`       | ANY        | course-service :8082 | `/courses/**`       | Rewrite path         |
| `/api/registrations`    | ANY        | registration-service :8083 | `/registrations` | Rewrite path   |
| `/api/registrations/**` | ANY        | registration-service :8083 | `/registrations/**` | Rewrite path |
| `/api/public/courses`   | GET        | course-service :8082 | `/courses`          | Partner API, cần X-API-KEY |

### Gateway Filter

#### AuthHeaderFilter (order = -1)

Kiểm tra sự tồn tại của header `Authorization` trên các route yêu cầu xác thực.

| Path / Điều kiện                          | Hành động                  |
|-------------------------------------------|----------------------------|
| `POST /api/auth/login`                    | Bỏ qua kiểm tra (open)     |
| `GET /api/public/courses`                 | Bỏ qua kiểm tra (open)     |
| `GET /api/courses` hoặc `GET /api/courses/**` | Bỏ qua kiểm tra (public read) |
| Còn lại, không có header `Authorization`  | `401 Unauthorized`         |
| Còn lại, có header `Authorization`        | Cho qua, service tự verify |

> **Lưu ý:** Gateway chỉ kiểm tra *sự tồn tại* của header, **không** verify chữ ký JWT. Việc verify JWT và kiểm tra role do từng service tự xử lý.

#### ApiKeyFilter (order = -2, chạy trước AuthHeaderFilter)

Áp dụng **chỉ** cho route `/api/public/courses`.

| Header `X-API-KEY`         | Kết quả         |
|----------------------------|-----------------|
| `crs-partner-key-2026`     | `200 OK`        |
| Sai hoặc không có          | `403 Forbidden` |

### CORS

Cấu hình tại Gateway (không tại từng service):

```yaml
allowedOrigins:
  - http://localhost:5173
  - http://localhost:5174
allowedMethods: "*"
allowedHeaders: "*"
```

---

## 3. Auth API

Backend: `auth-service` (:8081)

### POST /api/auth/login

Đăng nhập, nhận JWT token.

**Gateway URL:** `POST http://localhost:8080/api/auth/login`  
**Backend URL:** `POST http://localhost:8081/auth/login`  
**Authorization:** Không yêu cầu

#### Request Body

```json
{
  "username": "admin",
  "password": "admin123"
}
```

| Field      | Type   | Bắt buộc | Mô tả          |
|------------|--------|-----------|----------------|
| `username` | String | ✅        | Tên đăng nhập  |
| `password` | String | ✅        | Mật khẩu       |

#### Response – 200 OK

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

| Field      | Type   | Mô tả                          |
|------------|--------|--------------------------------|
| `token`    | String | JWT Bearer token (HS256)       |
| `username` | String | Tên đăng nhập                  |
| `role`     | String | `ADMIN` hoặc `STUDENT`         |

#### Response – 401 Unauthorized

Sai username hoặc password:

```json
{
  "timestamp": "2026-08-20T09:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Sai username hoac password",
  "path": "/auth/login"
}
```

#### JWT Properties

| Thuộc tính        | Giá trị                                                         |
|-------------------|-----------------------------------------------------------------|
| Algorithm         | HS256                                                           |
| Subject (sub)     | username                                                        |
| Claim `role`      | `ADMIN` hoặc `STUDENT`                                          |
| Expiration        | 86400000 ms (24 giờ)                                            |
| Secret (shared)   | `CRS-Microservices-Secret-Key-Nam-3-Hoc-Ky-2026-Doi-Trong-Thuc-Te` |

#### Tài khoản mẫu (DataSeeder)

| Username   | Password     | Role    |
|------------|--------------|---------|
| `admin`    | `admin123`   | ADMIN   |
| `student1` | `student123` | STUDENT |

---

## 4. Course API

Backend: `course-service` (:8082)

### GET /api/courses

Lấy danh sách môn học (có phân trang, có tìm kiếm).

**Gateway URL:** `GET http://localhost:8080/api/courses`  
**Backend URL:** `GET http://localhost:8082/courses`  
**Authorization:** Không yêu cầu (public)

#### Query Parameters

| Tham số   | Bắt buộc | Mô tả                               | Mặc định |
|-----------|----------|-------------------------------------|----------|
| `keyword` | Không    | Tìm kiếm theo tên môn học (LIKE)    | —        |
| `page`    | Không    | Số trang (0-indexed)                | 0        |
| `size`    | Không    | Số phần tử mỗi trang                | 20       |
| `sort`    | Không    | Sắp xếp (Spring Pageable format)    | —        |

#### Response – 200 OK (paginated)

```json
{
  "content": [
    {
      "id": 1,
      "tenMonHoc": "Lap Trinh Huong Doi Tuong",
      "soTinChi": 3,
      "soChoToiDa": 50,
      "soChoConLai": 48
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### GET /api/courses/{id}

Lấy chi tiết một môn học.

**Gateway URL:** `GET http://localhost:8080/api/courses/{id}`  
**Backend URL:** `GET http://localhost:8082/courses/{id}`  
**Authorization:** Không yêu cầu (public)

#### Response – 200 OK

```json
{
  "id": 1,
  "tenMonHoc": "Lap Trinh Huong Doi Tuong",
  "soTinChi": 3,
  "soChoToiDa": 50,
  "soChoConLai": 48
}
```

#### Response – 404 Not Found

```json
{
  "message": "Khong tim thay mon hoc id = 1"
}
```

---

### POST /api/courses

Tạo môn học mới.

**Gateway URL:** `POST http://localhost:8080/api/courses`  
**Backend URL:** `POST http://localhost:8082/courses`  
**Authorization:** `Bearer <JWT>` – Yêu cầu role **ADMIN**

#### Request Body

```json
{
  "tenMonHoc": "Kien Truc Microservices",
  "soTinChi": 3,
  "soChoToiDa": 40
}
```

| Field         | Type    | Bắt buộc | Validation             |
|---------------|---------|-----------|------------------------|
| `tenMonHoc`   | String  | ✅        | Không được để trống    |
| `soTinChi`    | Integer | ✅        | ≥ 1                    |
| `soChoToiDa`  | Integer | ✅        | ≥ 1                    |
| `soChoConLai` | Integer | Không     | Tự động = `soChoToiDa` |

#### Response – 201 Created

```json
{
  "id": 6,
  "tenMonHoc": "Kien Truc Microservices",
  "soTinChi": 3,
  "soChoToiDa": 40,
  "soChoConLai": 40
}
```

#### Lỗi thường gặp

| Status | Trường hợp                         |
|--------|------------------------------------|
| 400    | Tên môn học đã tồn tại             |
| 400    | Validation thất bại (field errors) |
| 401    | Thiếu header Authorization         |
| 403    | Role không phải ADMIN              |

---

### PUT /api/courses/{id}

Cập nhật môn học.

**Gateway URL:** `PUT http://localhost:8080/api/courses/{id}`  
**Backend URL:** `PUT http://localhost:8082/courses/{id}`  
**Authorization:** `Bearer <JWT>` – Yêu cầu role **ADMIN**

#### Request Body

```json
{
  "tenMonHoc": "Kien Truc Microservices (Cap Nhat)",
  "soTinChi": 4,
  "soChoToiDa": 45
}
```

> **Lưu ý:** PUT không cập nhật `soChoConLai` tự động. Chỉ cập nhật `tenMonHoc`, `soTinChi`, `soChoToiDa`.

#### Response – 200 OK

```json
{
  "id": 6,
  "tenMonHoc": "Kien Truc Microservices (Cap Nhat)",
  "soTinChi": 4,
  "soChoToiDa": 45,
  "soChoConLai": 40
}
```

---

### DELETE /api/courses/{id}

Xóa môn học.

**Gateway URL:** `DELETE http://localhost:8080/api/courses/{id}`  
**Backend URL:** `DELETE http://localhost:8082/courses/{id}`  
**Authorization:** `Bearer <JWT>` – Yêu cầu role **ADMIN**

#### Response – 204 No Content

Không có body.

#### Response – 404 Not Found

```json
{
  "message": "Khong tim thay mon hoc id = 1"
}
```

---

### Tóm tắt RBAC – Course API

| Method | Gateway URL           | Backend URL         | Authorization       |
|--------|-----------------------|---------------------|---------------------|
| GET    | `/api/courses`        | `/courses`          | Public (không cần token) |
| GET    | `/api/courses/{id}`   | `/courses/{id}`     | Public (không cần token) |
| POST   | `/api/courses`        | `/courses`          | JWT + role ADMIN    |
| PUT    | `/api/courses/{id}`   | `/courses/{id}`     | JWT + role ADMIN    |
| DELETE | `/api/courses/{id}`   | `/courses/{id}`     | JWT + role ADMIN    |

---

## 5. Registration API

Backend: `registration-service` (:8083)

Tất cả endpoint `/registrations/**` yêu cầu người dùng đã đăng nhập (JWT hợp lệ). Không phân biệt role ADMIN hay STUDENT.

### POST /api/registrations

Đăng ký học phần.

**Gateway URL:** `POST http://localhost:8080/api/registrations`  
**Backend URL:** `POST http://localhost:8083/registrations`  
**Authorization:** `Bearer <JWT>` – Bất kỳ role nào (ADMIN hoặc STUDENT)

#### Request Body

```json
{
  "studentId": 1,
  "courseId": 3
}
```

| Field       | Type | Bắt buộc | Mô tả                     |
|-------------|------|-----------|---------------------------|
| `studentId` | Long | ✅        | ID sinh viên              |
| `courseId`  | Long | ✅        | ID môn học cần đăng ký    |

#### Luồng xử lý nội bộ

```text
POST /api/registrations
        │
        ├─ Kiểm tra sinh viên đã đăng ký môn này chưa
        │   (trạng thái = DA_DANG_KY)
        │
        ├─ Gọi course-service internal API
        │   PATCH /internal/courses/{courseId}/reserve-seat
        │   (trừ 1 soChoConLai)
        │
        └─ Lưu Registration vào registration_db
```

#### Response – 201 Created

```json
{
  "id": 10,
  "studentId": 1,
  "courseId": 3,
  "trangThai": "DA_DANG_KY",
  "ngayDangKy": "2026-08-20T09:30:00"
}
```

| Field        | Type   | Mô tả                              |
|--------------|--------|------------------------------------|
| `id`         | Long   | ID của bản ghi đăng ký             |
| `studentId`  | Long   | ID sinh viên                       |
| `courseId`   | Long   | ID môn học                         |
| `trangThai`  | String | `DA_DANG_KY` hoặc `DA_HUY`        |
| `ngayDangKy` | String | Thời điểm đăng ký (LocalDateTime)  |

#### Lỗi thường gặp

| Status | Trường hợp                                       |
|--------|--------------------------------------------------|
| 400    | `studentId` hoặc `courseId` null                 |
| 401    | Thiếu hoặc hết hạn JWT                           |
| 409    | Sinh viên đã đăng ký môn này rồi                 |
| 409    | Môn học đã hết chỗ (từ course-service)           |
| 409    | Môn học không tồn tại (từ course-service)        |
| 409    | Không kết nối được course-service                |

---

### DELETE /api/registrations/{id}

Hủy đăng ký học phần.

**Gateway URL:** `DELETE http://localhost:8080/api/registrations/{id}`  
**Backend URL:** `DELETE http://localhost:8083/registrations/{id}`  
**Authorization:** `Bearer <JWT>` – Bất kỳ role nào (ADMIN hoặc STUDENT)

#### Luồng xử lý nội bộ

```text
DELETE /api/registrations/{id}
        │
        ├─ Tìm bản ghi Registration theo id
        │
        ├─ Kiểm tra trạng thái ≠ DA_HUY
        │
        ├─ Gọi course-service internal API
        │   PATCH /internal/courses/{courseId}/release-seat
        │   (hoàn lại 1 soChoConLai)
        │
        └─ Đổi trangThai = DA_HUY, lưu lại
```

#### Response – 200 OK

Không có body (void).

#### Lỗi thường gặp

| Status | Trường hợp                           |
|--------|--------------------------------------|
| 401    | Thiếu hoặc hết hạn JWT               |
| 404    | Không tìm thấy đăng ký với id này    |
| 409    | Đăng ký đã bị hủy trước đó           |

---

## 6. Partner Public API

Route đặc biệt cho đối tác bên ngoài. Không yêu cầu JWT, thay vào đó dùng API Key.

### GET /api/public/courses

Lấy danh sách môn học (dùng cho đối tác / third-party).

**Gateway URL:** `GET http://localhost:8080/api/public/courses`  
**Backend URL:** `GET http://localhost:8082/courses`  
**Authorization:** **Không** dùng JWT

#### Yêu cầu Header

```
X-API-KEY: crs-partner-key-2026
```

#### Response – 200 OK

Giống `GET /api/courses` – trả về `Page<CourseDTO>`:

```json
{
  "content": [
    {
      "id": 1,
      "tenMonHoc": "Lap Trinh Huong Doi Tuong",
      "soTinChi": 3,
      "soChoToiDa": 50,
      "soChoConLai": 48
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

#### Response – 403 Forbidden

Thiếu hoặc sai `X-API-KEY`:

```text
HTTP 403 Forbidden
(không có body)
```

---

## 7. Internal API (Service-to-Service)

Các endpoint này **không được expose qua API Gateway**. Chỉ `registration-service` gọi trực tiếp vào `course-service` qua mạng nội bộ (`http://localhost:8082`).

`SecurityConfig` của `course-service` cấu hình `/internal/**` là `permitAll()` – không yêu cầu JWT.

### PATCH /internal/courses/{id}/reserve-seat

Trừ 1 chỗ còn lại của môn học (giữ chỗ khi đăng ký).

**URL nội bộ:** `PATCH http://localhost:8082/internal/courses/{id}/reserve-seat`  
**Gọi bởi:** `registration-service` (qua `CourseClient`)  
**Authorization:** Không (internal network)

#### Response – 200 OK

Trả về `CourseDTO` sau khi trừ chỗ:

```json
{
  "id": 3,
  "tenMonHoc": "Cau Truc Du Lieu",
  "soTinChi": 3,
  "soChoToiDa": 30,
  "soChoConLai": 27
}
```

#### Response – 409 Conflict

```json
{
  "message": "Mon hoc da het cho, khong the dang ky"
}
```

---

### PATCH /internal/courses/{id}/release-seat

Hoàn lại 1 chỗ của môn học (khi hủy đăng ký).

**URL nội bộ:** `PATCH http://localhost:8082/internal/courses/{id}/release-seat`  
**Gọi bởi:** `registration-service` (qua `CourseClient`)  
**Authorization:** Không (internal network)

#### Response – 200 OK

Trả về `CourseDTO` sau khi hoàn chỗ.

---

## 8. Authentication & Authorization

### Cơ chế

1. **Đăng nhập** → `POST /api/auth/login` → auth-service phát hành JWT (HS256, 24h).
2. **Gửi request** → Đính kèm `Authorization: Bearer <token>` trong header.
3. **Gateway** (AuthHeaderFilter) → Kiểm tra header *có tồn tại không* với các route cần xác thực.
4. **Backend service** (JwtAuthFilter) → Tự verify chữ ký JWT, đọc `sub` và `role`, thiết lập SecurityContext.
5. **Spring Security** tại service → Kiểm tra role và từ chối nếu không đủ quyền.

### Phân quyền theo Role

| Role    | Quyền                                                                    |
|---------|--------------------------------------------------------------------------|
| ADMIN   | Tất cả Course API (GET/POST/PUT/DELETE) + Registration API               |
| STUDENT | Chỉ GET Course API (public) + Registration API (POST/DELETE)             |
| Không có token | GET Course API (public) + GET `/api/public/courses` với API Key |

### JWT Claims

```json
{
  "sub": "admin",
  "role": "ADMIN",
  "iat": 1724140800,
  "exp": 1724227200
}
```

---

## 9. Error Response

### auth-service (GlobalExceptionHandler)

Format lỗi có đầy đủ metadata:

```json
{
  "timestamp": "2026-08-20T09:00:00.000Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Sai username hoac password",
  "path": "/auth/login"
}
```

| Exception                    | HTTP Status | Trường hợp         |
|------------------------------|-------------|--------------------|
| `InvalidCredentialsException`| 401         | Sai username/pass  |
| `Exception` (generic)        | 500         | Lỗi không xác định |

---

### course-service (GlobalExceptionHandler)

Format lỗi ngắn gọn:

```json
{
  "message": "Khong tim thay mon hoc id = 5"
}
```

Lỗi validation (nhiều field):

```json
{
  "tenMonHoc": "Ten mon hoc khong duoc de trong",
  "soTinChi": "So tin chi phai lon hon 0"
}
```

| Exception                      | HTTP Status | Trường hợp                          |
|--------------------------------|-------------|-------------------------------------|
| `NoSuchElementException`       | 404         | Không tìm thấy môn học              |
| `IllegalArgumentException`     | 400         | Tên môn học đã tồn tại              |
| `MethodArgumentNotValidException` | 400      | Validation DTO thất bại             |
| `IllegalStateException`        | 409         | Hết chỗ (conflict)                  |

---

### registration-service (GlobalExceptionHandler)

Format lỗi ngắn gọn:

```json
{
  "message": "Sinh vien da dang ky mon hoc nay roi"
}
```

| Exception                      | HTTP Status | Trường hợp                          |
|--------------------------------|-------------|-------------------------------------|
| `NoSuchElementException`       | 404         | Không tìm thấy đăng ký              |
| `IllegalStateException`        | 409         | Đã đăng ký, đã hủy, hết chỗ        |
| `MethodArgumentNotValidException` | 400      | Validation DTO thất bại             |

---

## 10. Ví dụ cURL

### Đăng nhập ADMIN

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

### Lấy danh sách môn học (không cần token)

```bash
curl http://localhost:8080/api/courses
```

### Lấy danh sách môn học có tìm kiếm và phân trang

```bash
curl "http://localhost:8080/api/courses?keyword=lap+trinh&page=0&size=5"
```

### Tạo môn học (ADMIN)

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "tenMonHoc": "Kien Truc Microservices",
    "soTinChi": 3,
    "soChoToiDa": 40
  }'
```

### Đăng ký học phần (STUDENT)

```bash
curl -X POST http://localhost:8080/api/registrations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <STUDENT_TOKEN>" \
  -d '{
    "studentId": 1,
    "courseId": 3
  }'
```

### Hủy đăng ký

```bash
curl -X DELETE http://localhost:8080/api/registrations/10 \
  -H "Authorization: Bearer <TOKEN>"
```

### Partner Public API (X-API-KEY)

```bash
curl http://localhost:8080/api/public/courses \
  -H "X-API-KEY: crs-partner-key-2026"
```
