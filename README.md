# CRS Microservices

Repository thực hành môn Kiến trúc Microservices.

---

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

| Service        | Port | Database |
|----------------|------|----------|
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
|--------|----------|------|
| GET | `/courses` | Lấy danh sách môn học |
| GET | `/courses/{id}` | Lấy môn học theo ID |
| POST | `/courses` | Thêm môn học |
| PUT | `/courses/{id}` | Cập nhật môn học |
| DELETE | `/courses/{id}` | Xóa môn học |

---

## Lab 03 - Registration Service & Communication Between Services

### Nội dung

Phát triển `registration-service` và thực hiện giao tiếp giữa các microservice.

### Chức năng

#### Course Service

- Tìm kiếm môn học.
- Phân trang danh sách môn học.
- API nội bộ giữ chỗ.
- API nội bộ trả lại chỗ.
- Quản lý số chỗ còn lại của môn học.

#### Registration Service

- Tạo đăng ký môn học.
- Lấy danh sách đăng ký.
- Hủy đăng ký.
- Kiểm tra sinh viên đã đăng ký môn học hay chưa.
- Kiểm tra môn học có tồn tại hay không.
- Kiểm tra môn học còn chỗ hay không.
- Gọi `course-service` thông qua REST API.
- Xử lý lỗi khi `course-service` không khả dụng.
- Global Exception Handler trả về JSON lỗi.

### Kiến trúc

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
