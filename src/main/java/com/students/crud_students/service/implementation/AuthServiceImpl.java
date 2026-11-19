package com.students.crud_students.service.implementation;

import com.students.crud_students.dto.AuthResponseDTO;
import com.students.crud_students.dto.LoginRequestDTO;
import com.students.crud_students.dto.RefreshTokenRequestDTO;
import com.students.crud_students.model.User;
import com.students.crud_students.repository.UserRepository;
import com.students.crud_students.security.CustomUserPrincipal;
import com.students.crud_students.security.JwtService;
import com.students.crud_students.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        List<String> roles = extractSortedRoles(principal);

        String accessToken = jwtService.generateAccessToken(
                principal.getEmail(),
                principal.getUserId(),
                principal.getStudentId(),
                roles
        );
        String refreshToken = jwtService.generateRefreshToken(principal.getEmail());

        log.info("User '{}' authenticated successfully (userId={}, studentId={})",
                principal.getEmail(), principal.getUserId(), principal.getStudentId());

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationInSeconds(),
                principal.getUserId(),
                principal.getEmail(),
                principal.getStudentId(),
                roles
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String rawRefreshToken = request.refreshToken().trim();

        if (!jwtService.isRefreshTokenValid(rawRefreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String normalizedEmail = jwtService.extractEmail(rawRefreshToken).trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("User associated with refresh token no longer exists"));

        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        CustomUserPrincipal principal = CustomUserPrincipal.fromUser(user);
        List<String> roles = extractSortedRoles(principal);

        String newAccessToken = jwtService.generateAccessToken(
                principal.getEmail(),
                principal.getUserId(),
                principal.getStudentId(),
                roles
        );
        String newRefreshToken = jwtService.generateRefreshToken(principal.getEmail());

        log.info("Access token refreshed for user '{}' (userId={})", principal.getEmail(), principal.getUserId());

        return new AuthResponseDTO(
                newAccessToken,
                newRefreshToken,
                jwtService.getAccessTokenExpirationInSeconds(),
                principal.getUserId(),
                principal.getEmail(),
                principal.getStudentId(),
                roles
        );
    }

    private List<String> extractSortedRoles(CustomUserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
    }
}