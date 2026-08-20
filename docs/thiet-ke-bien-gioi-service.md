# Thiết kế Biên giới Service – CRS Microservices

> Tài liệu mô tả trách nhiệm, phạm vi, và ranh giới của từng service trong hệ thống CRS.  
> Nguồn sự thật: code thực tế trong repository (Lab 01 → Lab 05).

---

## Mục lục

1. [Nguyên tắc thiết kế](#1-nguyên-tắc-thiết-kế)
2. [Auth Service Boundary](#2-auth-service-boundary)
3. [Course Service Boundary](#3-course-service-boundary)
4. [Registration Service Boundary](#4-registration-service-boundary)
5. [API Gateway Boundary](#5-api-gateway-boundary)
6. [Frontend Boundary](#6-frontend-boundary)
7. [Service-to-Service Communication](#7-service-to-service-communication)
8. [Security Boundary](#8-security-boundary)
9. [Database Boundary](#9-database-boundary)
10. [CORS Boundary](#10-cors-boundary)
11. [Những gì KHÔNG được phép](#11-những-gì-không-được-phép)

---

## 1. Nguyên tắc thiết kế

| Nguyên tắc | Mô tả |
|------------|-------|
| **Single Responsibility** | Mỗi service có trách nhiệm nghiệp vụ rõ ràng, không gánh việc của service khác |
| **Database per Service** | Mỗi service sở hữu database riêng, không truy cập DB của service khác |
| **API Gateway as Entry Point** | Client bên ngoài chỉ gọi qua Gateway, không biết địa chỉ nội bộ |
| **Defense in Depth** | Mỗi service tự verify JWT, không tin tưởng tuyệt đối vào Gateway |
| **Loose Coupling** | Service giao tiếp qua REST API, không share code hay DB |

---

## 2. Auth Service Boundary

### Thông tin

| Thuộc tính | Giá trị      |
|------------|--------------|
| Port       | 8081         |
| Database   | course_db ¹  |
| Framework  | Spring Boot, Spring Security |

> ¹ Theo `application.properties` hiện tại trong môi trường lab, `auth-service` đang kết nối vào `course_db`. Trên môi trường production nên dùng DB riêng `auth_db`.

### Trách nhiệm

- Xác thực người dùng (username / password).
- Kiểm tra mật khẩu bằng **BCrypt**.
- Phát hành **JWT** (HS256, 24h) chứa `username` và `role`.
- Seed tài khoản mẫu qua `DataSeeder`.

### Không chịu trách nhiệm

- Quản lý CRUD môn học.
- Quản lý đăng ký học phần.
- Verify JWT của request đến – `auth-service` chỉ **tạo** JWT, không verify lại.

### API Boundary (internal port 8081)

| Method | Endpoint      | Mô tả                     |
|--------|---------------|---------------------------|
| POST   | `/auth/login` | Đăng nhập, nhận JWT token |

> Chỉ có **1 endpoint** duy nhất. Không có `/auth/register`, `/auth/profile`, v.v.

### Entity

- **`User`** (bảng `app_user`): `id`, `username`, `password` (BCrypt), `role`
- **`Student`** (bảng `student`): `id`, `hoTen`, `mssv`, `user` (FK)

### Security Config

`auth-service` mở toàn bộ endpoint (`permitAll`). Không có JWT filter – đây là nơi phát hành token, không phải nơi verify.

```
auth-service SecurityConfig:
  .authorizeHttpRequests → anyRequest().permitAll()
```

---

## 3. Course Service Boundary

### Thông tin

| Thuộc tính | Giá trị      |
|------------|--------------|
| Port       | 8082         |
| Database   | course_db    |
| Framework  | Spring Boot, Spring Security + JwtAuthFilter |

### Trách nhiệm

- CRUD môn học (Create, Read, Update, Delete).
- Tìm kiếm môn học theo từ khóa (LIKE, case-insensitive).
- Phân trang danh sách môn học (Spring `Pageable`).
- Quản lý `soChoConLai` (số chỗ còn lại).
- Cung cấp **Internal API** để `registration-service` gọi nội bộ (giữ chỗ / hoàn chỗ).

### Không chịu trách nhiệm

- Xác thực người dùng.
- Quản lý đăng ký học phần.
- Phát hành JWT.

### API Boundary

**Public API** (expose qua Gateway):

| Method | Endpoint       | Authorization        |
|--------|----------------|----------------------|
| GET    | `/courses`     | Public (không token) |
| GET    | `/courses/{id}`| Public (không token) |
| POST   | `/courses`     | JWT + role ADMIN     |
| PUT    | `/courses/{id}`| JWT + role ADMIN     |
| DELETE | `/courses/{id}`| JWT + role ADMIN     |

**Internal API** (không expose qua Gateway, chỉ dùng nội bộ):

| Method | Endpoint                            | Authorization |
|--------|-------------------------------------|---------------|
| PATCH  | `/internal/courses/{id}/reserve-seat` | Không (internal, permitAll) |
| PATCH  | `/internal/courses/{id}/release-seat` | Không (internal, permitAll) |

### Security Config

```
course-service SecurityConfig:
  /internal/**          → permitAll()
  GET /courses/**       → permitAll()
  POST /courses/**      → hasRole("ADMIN")
  PUT /courses/**       → hasRole("ADMIN")
  DELETE /courses/**    → hasRole("ADMIN")
  anyRequest            → authenticated()
```

### Entity

- **`Course`** (bảng `course`): `id`, `tenMonHoc`, `soTinChi`, `soChoToiDa`, `soChoConLai`

---

## 4. Registration Service Boundary

### Thông tin

| Thuộc tính | Giá trị          |
|------------|------------------|
| Port       | 8083             |
| Database   | registration_db  |
| Framework  | Spring Boot, Spring Security + JwtAuthFilter |

### Trách nhiệm

- Tạo đăng ký học phần (POST).
- Hủy đăng ký học phần (DELETE).
- Kiểm tra sinh viên đã đăng ký môn học chưa.
- Gọi **nội bộ** sang `course-service` để giữ chỗ / hoàn chỗ.
- Xử lý lỗi khi `course-service` không khả dụng.

### Không chịu trách nhiệm

- Xác thực người dùng.
- CRUD môn học.
- Phát hành JWT.
- Verify JWT của `auth-service` (tự verify độc lập).

### API Boundary

| Method | Endpoint             | Authorization                     |
|--------|----------------------|-----------------------------------|
| POST   | `/registrations`     | JWT hợp lệ (ADMIN hoặc STUDENT)  |
| DELETE | `/registrations/{id}`| JWT hợp lệ (ADMIN hoặc STUDENT)  |

> `registration-service` **không có** `GET /registrations`.

### Security Config

```
registration-service SecurityConfig:
  /registrations/**   → authenticated()
  anyRequest          → permitAll()
```

### Entity

- **`Registration`** (bảng `registration`): `id`, `studentId`, `courseId`, `trangThai` (`DA_DANG_KY` / `DA_HUY`), `ngayDangKy`

### Giao tiếp nội bộ

`registration-service` dùng `RestTemplate` qua `CourseClient` để gọi:

```
http://localhost:8082/internal/courses/{courseId}/reserve-seat  (PATCH)
http://localhost:8082/internal/courses/{courseId}/release-seat  (PATCH)
```

URL được cấu hình qua:

```properties
course-service.base-url=http://localhost:8082
```

---

## 5. API Gateway Boundary

### Thông tin

| Thuộc tính | Giá trị                         |
|------------|---------------------------------|
| Port       | 8080                            |
| Framework  | Spring Cloud Gateway (Reactive / WebFlux) |
| Database   | Không có                        |

### Trách nhiệm

- **Entry point** duy nhất cho client bên ngoài.
- **Routing** request tới đúng backend service.
- **Rewrite path** (ví dụ: `/api/auth/login` → `/auth/login`).
- **AuthHeaderFilter**: Kiểm tra sự tồn tại của header `Authorization` (không verify JWT).
- **ApiKeyFilter**: Kiểm tra `X-API-KEY` cho Partner API.
- **CORS**: Cấu hình cho frontend `http://localhost:5173`.

### Không chịu trách nhiệm

- **Không** verify chữ ký JWT (việc đó do từng service).
- **Không** xử lý business logic (course, registration).
- **Không** kết nối database.

### Routing Table

| Gateway Path            | Rewrite thành       | Backend          |
|-------------------------|---------------------|------------------|
| `/api/auth/**`          | `/auth/**`          | :8081            |
| `/api/courses`          | `/courses`          | :8082            |
| `/api/courses/**`       | `/courses/**`       | :8082            |
| `/api/registrations`    | `/registrations`    | :8083            |
| `/api/registrations/**` | `/registrations/**` | :8083            |
| `/api/public/courses`   | `/courses`          | :8082            |

### Filter Order

| Filter            | Order | Mục đích                    |
|-------------------|-------|-----------------------------|
| `ApiKeyFilter`    | -2    | Kiểm tra X-API-KEY (chạy trước) |
| `AuthHeaderFilter`| -1    | Kiểm tra header Authorization   |

---

## 6. Frontend Boundary

### Thông tin

| Thuộc tính      | Giá trị                   |
|-----------------|---------------------------|
| Framework       | Vite + React + TypeScript |
| Dev server port | 5173 (mặc định Vite)      |
| API base URL    | `http://localhost:8080`   |

### Nguyên tắc

- Frontend **chỉ biết** Gateway ở `http://localhost:8080`.
- Không hardcode port 8081, 8082, 8083.
- API base URL được đọc từ biến môi trường:

```env
VITE_API_BASE_URL=http://localhost:8080
```

- Axios instance (`axiosClient`) đọc `import.meta.env.VITE_API_BASE_URL`.

### API đã tích hợp (Lab 05)

| Function      | Method | Gateway URL     | Mô tả                    |
|---------------|--------|-----------------|--------------------------|
| `getCourses`  | GET    | `/api/courses`  | Lấy danh sách môn học (có pagination, keyword) |

### TypeScript Types

```typescript
// course.ts
interface Course {
  id: number;
  tenMonHoc: string;
  soTinChi: number;
  soChoToiDa: number;
  soChoConLai: number;
}

interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// auth.ts
interface LoginRequest { username: string; password: string; }
interface LoginResponse { token: string; username: string; role: 'ADMIN' | 'STUDENT'; }

// registration.ts
interface Registration {
  id: number; studentId: number; courseId: number;
  trangThai: 'DA_DANG_KY' | 'DA_HUY'; ngayDangKy: string;
}
```

---

## 7. Service-to-Service Communication

### Luồng tổng thể

```text
crs-frontend (:5173)
        │
        │  HTTP (qua VITE_API_BASE_URL = localhost:8080)
        ▼
API Gateway (:8080)
        │
        ├─ /api/auth/**      ──► auth-service        (:8081)
        ├─ /api/courses/**   ──► course-service       (:8082)
        └─ /api/registrations/** ──► registration-service (:8083)
```

### Giao tiếp nội bộ (không qua Gateway)

```text
registration-service (:8083)
        │
        │  RestTemplate (HTTP trực tiếp, không qua Gateway)
        │
        ├─ PATCH http://localhost:8082/internal/courses/{id}/reserve-seat
        └─ PATCH http://localhost:8082/internal/courses/{id}/release-seat
```

> **Quan trọng:** `registration-service` gọi thẳng vào port 8082, **không** đi qua Gateway (:8080). Endpoint `/internal/**` không được expose trong routing table của Gateway.

### Xử lý lỗi giao tiếp nội bộ

| Lỗi từ course-service       | registration-service xử lý như      |
|-----------------------------|--------------------------------------|
| 409 Conflict (hết chỗ)      | `IllegalStateException("Mon hoc da het cho")` → 409 |
| 404 Not Found (không có môn)| `IllegalStateException("Mon hoc khong ton tai")` → 409 |
| Server Error / Timeout      | `IllegalStateException("Khong the ket noi toi course-service...")` → 409 |

---

## 8. Security Boundary

### Hai lớp bảo vệ độc lập

```text
Layer 1 – API Gateway
─────────────────────────────────────────────────────────
  Thiếu header Authorization (với route cần xác thực)
      → 401 Unauthorized (Gateway trả về, không forward)

  Sai X-API-KEY (với /api/public/courses)
      → 403 Forbidden (Gateway trả về, không forward)

  GET /api/courses/... hoặc POST /api/auth/login
      → Cho qua không cần Authorization


Layer 2 – Backend Service (course-service, registration-service)
─────────────────────────────────────────────────────────
  JwtAuthFilter tự verify chữ ký JWT bằng shared secret
  Tự đọc claim "role" từ JWT payload
  Spring Security kiểm tra role:
      POST/PUT/DELETE /courses/** yêu cầu ROLE_ADMIN → 403 nếu STUDENT
      /registrations/** yêu cầu authenticated → 401 nếu không có JWT
```

### Ví dụ minh họa

**Case 1: Gateway chặn – thiếu Authorization**

```text
POST /api/courses (không có Authorization header)
        │
        ▼
API Gateway – AuthHeaderFilter
        │  Authorization == null
        ▼
401 Unauthorized  ← Gateway trả về, không forward tới course-service
```

**Case 2: Gateway cho qua – Service từ chối do Role**

```text
POST /api/courses
Authorization: Bearer <STUDENT_TOKEN>
        │
        ▼
API Gateway – AuthHeaderFilter
        │  Authorization có → cho qua
        ▼
course-service – JwtAuthFilter
        │  Verify JWT → hợp lệ
        │  role = STUDENT
        ▼
Spring Security
        │  POST yêu cầu ROLE_ADMIN
        ▼
403 Forbidden  ← course-service trả về
```

---

## 9. Database Boundary

| Service              | Database        | Bảng chính                   | Sở hữu bởi           |
|----------------------|-----------------|------------------------------|----------------------|
| auth-service         | course_db ¹     | `app_user`, `student`        | auth-service         |
| course-service       | course_db       | `course`                     | course-service       |
| registration-service | registration_db | `registration`               | registration-service |

> ¹ Trong môi trường lab, `auth-service` và `course-service` đang chia sẻ cùng `course_db`. Đây là thiết lập học tập; trên production mỗi service cần database riêng.

### Nguyên tắc

- Service A **không** truy cập trực tiếp bảng của Service B bằng SQL/JPA.
- Nếu cần dữ liệu của service khác → gọi REST API.
- `registration-service` không truy cập `course_db` – chỉ gọi `course-service` qua HTTP.

---

## 10. CORS Boundary

CORS được cấu hình **tập trung tại API Gateway**. Các backend service không cần cấu hình CORS riêng.

### Cấu hình hiện tại (application.yml của Gateway)

```yaml
spring:
  globalcors:
    cors-configurations:
      '[/**]':
        allowedOrigins:
          - "http://localhost:5173"
          - "http://localhost:5174"
        allowedMethods: "*"
        allowedHeaders: "*"
```

| Thuộc tính        | Giá trị                                      |
|-------------------|----------------------------------------------|
| Allowed Origins   | `http://localhost:5173`, `http://localhost:5174` |
| Allowed Methods   | Tất cả (GET, POST, PUT, DELETE, PATCH, ...)   |
| Allowed Headers   | Tất cả                                        |

> **Lưu ý:** `allowCredentials` không được cấu hình, nên cookie-based auth không hoạt động. Hệ thống dùng JWT trong Authorization header, không cần cookie.

---

## 11. Những gì KHÔNG được phép

### Frontend / Client

| ❌ Không được | ✅ Thay thế bằng |
|--------------|-----------------|
| Gọi trực tiếp `http://localhost:8081` (auth) | Gọi qua `http://localhost:8080/api/auth/...` |
| Gọi trực tiếp `http://localhost:8082` (course) | Gọi qua `http://localhost:8080/api/courses/...` |
| Gọi trực tiếp `http://localhost:8083` (registration) | Gọi qua `http://localhost:8080/api/registrations/...` |
| Hardcode port 8081/8082/8083 trong frontend | Dùng `VITE_API_BASE_URL` |

### Backend Service

| ❌ Không được | ✅ Thay thế bằng |
|--------------|-----------------|
| Truy cập trực tiếp DB của service khác | Gọi REST API của service đó |
| `registration-service` dùng JPA query vào `course_db` | Gọi `/internal/courses/{id}/reserve-seat` |
| Tin tưởng tuyệt đối Gateway đã verify JWT | Tự verify JWT bằng shared secret |
| Đặt business logic (course/registration) vào Gateway | Giữ logic tại service tương ứng |

### Thiết kế chung

| ❌ Không được | Lý do |
|--------------|-------|
| Expose `/internal/**` qua Gateway | Endpoint này chỉ dùng nội bộ, không phải public API |
| Dùng chung DB giữa hai service độc lập (trên production) | Vi phạm Database per Service principle |
| Đặt CORS tại từng backend service | CORS phải tập trung tại Gateway để tránh xung đột |
| Tin Gateway là đủ để bảo vệ – bỏ qua verify JWT tại service | Gateway có thể bị bypass; service phải tự bảo vệ |
