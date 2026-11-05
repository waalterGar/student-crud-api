package com.students.crud_students.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EnrollmentRequestDTO(
        @NotNull(message = "Student ID is required")
        @Positive(message = "Student ID must be greater than 0")
        Long studentId,

        @NotNull(message = "Course ID is required")
        @Positive(message = "Course ID must be greater than 0")
        Long courseId
) {}