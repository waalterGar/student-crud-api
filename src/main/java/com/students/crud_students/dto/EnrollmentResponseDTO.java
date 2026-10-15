package com.students.crud_students.dto;

import com.students.crud_students.model.EnrollmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnrollmentResponseDTO(
    Long id,
    Long studentId,
    String studentName,
    Long courseId,
    String courseCode,
    String courseTitle,
    EnrollmentStatus status,
    BigDecimal grade,
    LocalDateTime enrolledAt
) {}