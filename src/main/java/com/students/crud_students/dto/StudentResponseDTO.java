package com.students.crud_students.dto;

import com.students.crud_students.model.StudentStatus;

public record StudentResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        Integer age,
        StudentStatus status
) {}
