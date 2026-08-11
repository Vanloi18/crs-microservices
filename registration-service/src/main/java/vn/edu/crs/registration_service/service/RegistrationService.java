package vn.edu.crs.registration_service.service;

import vn.edu.crs.registration_service.client.CourseClient;
import vn.edu.crs.registration_service.dto.RegistrationRequestDTO;
import vn.edu.crs.registration_service.entity.Registration;
import vn.edu.crs.registration_service.repository.RegistrationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String DA_DANG_KY = "DA_DANG_KY";
    private static final String DA_HUY = "DA_HUY";

    private final RegistrationRepository registrationRepository;
    private final CourseClient courseClient;

    /**
     * Đăng ký học phần.
     */
    public Registration register(RegistrationRequestDTO dto) {

        // Kiểm tra sinh viên đã đăng ký môn này chưa
        if (registrationRepository.existsByStudentIdAndCourseIdAndTrangThai(
                dto.getStudentId(),
                dto.getCourseId(),
                DA_DANG_KY)) {

            throw new IllegalStateException(
                    "Sinh vien da dang ky mon hoc nay roi"
            );
        }

        // Bước 1:
        // Gọi course-service để trừ chỗ trước
        courseClient.reserveSeat(dto.getCourseId());

        // Bước 2:
        // Chỉ lưu Registration sau khi course-service
        // xác nhận trừ chỗ thành công
        Registration registration = new Registration();

        registration.setStudentId(dto.getStudentId());
        registration.setCourseId(dto.getCourseId());
        registration.setTrangThai(DA_DANG_KY);
        registration.setNgayDangKy(LocalDateTime.now());

        return registrationRepository.save(registration);
    }

    /**
     * Hủy đăng ký học phần.
     */
    public void cancel(Long registrationId) {

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Khong tim thay dang ky id = " + registrationId
                        )
                );

        // Không cho hủy một đăng ký đã hủy
        if (DA_HUY.equals(registration.getTrangThai())) {
            throw new IllegalStateException(
                    "Dang ky nay da duoc huy truoc do"
            );
        }

        // Gọi course-service để hoàn lại chỗ trước
        courseClient.releaseSeat(registration.getCourseId());

        // Sau khi hoàn chỗ thành công mới đổi trạng thái
        registration.setTrangThai(DA_HUY);

        registrationRepository.save(registration);
    }
}