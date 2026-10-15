package com.students.crud_students.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequestDTO(

    @NotNull(message = "Student ID is required")
    Long studentId,

    @NotNull(message = "Course ID is required")
    Long courseId
) {}