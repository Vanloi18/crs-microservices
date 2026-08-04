\# Thiết kế biên giới Service



\## 1. Danh sách Service



| Service | Cổng | Database | Trách nhiệm |

|----------|------|----------|-------------|

| api-gateway | 8080 | Không có | Gateway, định tuyến request |

| auth-service | 8081 | auth\_db | Đăng nhập, JWT, xác thực |

| course-service | 8082 | course\_db | Quản lý môn học |

| registration-service | 8083 | registration\_db | Đăng ký học phần |



\---



\## 2. Data Ownership



\- Mỗi service sở hữu database riêng.

\- Không truy cập trực tiếp database của service khác.

\- Muốn lấy dữ liệu phải gọi REST API.



Ví dụ:



registration-service



↓



course-service



↓



course\_db



\---



\## 3. Gateway Routing



| Route | Forward tới |

|--------|-------------|

| /api/auth/\*\* | http://localhost:8081 |

| /api/courses/\*\* | http://localhost:8082 |

| /api/registrations/\*\* | http://localhost:8083 |



\---



\## 4. Database



auth-service



↓



auth\_db



course-service



↓



course\_db



registration-service



↓



registration\_db

