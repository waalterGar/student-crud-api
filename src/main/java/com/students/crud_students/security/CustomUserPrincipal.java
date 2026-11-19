package com.students.crud_students.security;

import com.students.crud_students.model.User;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class CustomUserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Long studentId;
    private final Collection<? extends GrantedAuthority> authorities;

    public static CustomUserPrincipal fromUser(User user) {
        Long linkedStudentId = (user.getStudent() != null) ? user.getStudent().getId() : null;

        List<SimpleGrantedAuthority> grantedAuthorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .toList();

        return new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                linkedStudentId,
                grantedAuthorities
        );
    }

    public static CustomUserPrincipal fromClaims(Claims claims) {
        String subjectEmail = claims.getSubject();

        Number userIdNumber = claims.get(JwtService.CLAIM_USER_ID, Number.class);
        Long extractedUserId = (userIdNumber != null) ? userIdNumber.longValue() : null;

        Number studentIdNumber = claims.get(JwtService.CLAIM_STUDENT_ID, Number.class);
        Long extractedStudentId = (studentIdNumber != null) ? studentIdNumber.longValue() : null;

        List<?> rawRoles = claims.get(JwtService.CLAIM_ROLES, List.class);
        List<SimpleGrantedAuthority> grantedAuthorities = (rawRoles == null)
                ? Collections.emptyList()
                : rawRoles.stream()
                        .map(role -> new SimpleGrantedAuthority(role.toString()))
                        .toList();

        return new CustomUserPrincipal(
                extractedUserId,
                subjectEmail,
                null,
                true,
                extractedStudentId,
                grantedAuthorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}