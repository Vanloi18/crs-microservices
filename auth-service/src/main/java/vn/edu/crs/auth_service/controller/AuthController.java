package vn.edu.crs.auth_service.controller;

import vn.edu.crs.auth_service.dto.LoginRequestDTO;
import vn.edu.crs.auth_service.dto.LoginResponseDTO;
import vn.edu.crs.auth_service.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponseDTO login(
            @Valid @RequestBody LoginRequestDTO dto
    ) {
        return authService.login(dto);
    }
}