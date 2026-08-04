\# Blueprint API



\## auth-service



\### POST /auth/login



Đăng nhập.



\### POST /auth/register



Đăng ký tài khoản.



\### GET /auth/profile



Lấy thông tin người dùng.



\---



\## course-service



\### GET /courses



Danh sách môn học.



\### GET /courses/{id}



Chi tiết môn học.



\### POST /courses



Thêm môn học.



\### PUT /courses/{id}



Cập nhật môn học.



\### DELETE /courses/{id}



Xóa môn học.



\---



\## registration-service



\### POST /registrations



Đăng ký học phần.



\### GET /registrations



Danh sách đăng ký.



\### DELETE /registrations/{id}



Hủy đăng ký.



\---



\## Internal API



\### POST /internal/reserve-seat



Giữ chỗ.



\### POST /internal/release-seat



Trả chỗ.

