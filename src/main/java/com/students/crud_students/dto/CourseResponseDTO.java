package com.students.crud_students.dto;

import com.students.crud_students.model.CourseStatus;

import java.time.LocalDateTime;

public record CourseResponseDTO(
    Long id,
    String code,
    String title,
    Integer credits,
    Integer maxCapacity,
    Integer currentEnrolled,
    CourseStatus status,
    LocalDateTime createdAt
) {}