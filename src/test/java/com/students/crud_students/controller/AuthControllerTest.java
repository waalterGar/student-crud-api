package com.students.crud_students.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.students.crud_students.dto.AuthResponseDTO;
import com.students.crud_students.dto.LoginRequestDTO;
import com.students.crud_students.dto.RefreshTokenRequestDTO;
import com.students.crud_students.security.JwtService;
import com.students.crud_students.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.students.crud_students.config.SecurityConfig;
import com.students.crud_students.security.JwtAccessDeniedHandler;
import com.students.crud_students.security.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Import;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "springdoc.api-docs.enabled=false")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should return 200 with access and refresh tokens when credentials are valid")
        void shouldReturn200WhenLoginValid() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("admin@students.com", "password");
            AuthResponseDTO response = new AuthResponseDTO(
                    "mock-access-token",
                    "mock-refresh-token",
                    "Bearer",
                    900L,
                    1L,
                    "admin@students.com",
                    null,
                    List.of("ROLE_ADMIN")
            );

            when(authService.login(any(LoginRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", is("mock-access-token")))
                    .andExpect(jsonPath("$.refreshToken", is("mock-refresh-token")))
                    .andExpect(jsonPath("$.tokenType", is("Bearer")))
                    .andExpect(jsonPath("$.expiresIn", is(900)))
                    .andExpect(jsonPath("$.userId", is(1)))
                    .andExpect(jsonPath("$.email", is("admin@students.com")))
                    .andExpect(jsonPath("$.studentId").value(nullValue()))
                    .andExpect(jsonPath("$.roles", hasSize(1)))
                    .andExpect(jsonPath("$.roles[0]", is("ROLE_ADMIN")));
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid or password is blank")
        void shouldReturn400WhenRequestInvalid() throws Exception {
            LoginRequestDTO invalidRequest = new LoginRequestDTO("not-an-email", "");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.errors.email", notNullValue()))
                    .andExpect(jsonPath("$.errors.password", notNullValue()));
        }

        @Test
        @DisplayName("Should return 401 when credentials are invalid")
        void shouldReturn401WhenBadCredentials() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("admin@students.com", "wrong-password");
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error", is("Unauthorized")))
                    .andExpect(jsonPath("$.message", is("Invalid email or password, or user account is disabled")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("Should return 200 with rotated tokens when refresh token is valid")
        void shouldReturn200WhenRefreshTokenValid() throws Exception {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("valid-refresh-token");
            AuthResponseDTO response = new AuthResponseDTO(
                    "new-access-token",
                    "new-refresh-token",
                    "Bearer",
                    900L,
                    2L,
                    "carlos.mendoza@example.com",
                    1L,
                    List.of("ROLE_STUDENT")
            );

            when(authService.refreshToken(any(RefreshTokenRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", is("new-access-token")))
                    .andExpect(jsonPath("$.refreshToken", is("new-refresh-token")))
                    .andExpect(jsonPath("$.studentId", is(1)))
                    .andExpect(jsonPath("$.roles[0]", is("ROLE_STUDENT")));
        }

        @Test
        @DisplayName("Should return 400 when refresh token is blank")
        void shouldReturn400WhenRefreshTokenBlank() throws Exception {
            RefreshTokenRequestDTO invalidRequest = new RefreshTokenRequestDTO("   ");

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.errors.refreshToken", notNullValue()));
        }

        @Test
        @DisplayName("Should return 401 when refresh token is expired or invalid")
        void shouldReturn401WhenRefreshTokenInvalid() throws Exception {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("tampered-token");
            when(authService.refreshToken(any(RefreshTokenRequestDTO.class)))
                    .thenThrow(new BadCredentialsException("Invalid or expired refresh token"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error", is("Unauthorized")));
        }
    }
}