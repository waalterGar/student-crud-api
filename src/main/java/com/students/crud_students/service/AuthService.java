package com.students.crud_students.service;

import com.students.crud_students.dto.AuthResponseDTO;
import com.students.crud_students.dto.LoginRequestDTO;
import com.students.crud_students.dto.RefreshTokenRequestDTO;

public interface AuthService {

    AuthResponseDTO login(LoginRequestDTO request);

    AuthResponseDTO refreshToken(RefreshTokenRequestDTO request);
}