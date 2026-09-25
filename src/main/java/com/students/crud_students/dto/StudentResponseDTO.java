package com.students.crud_students.dto;

public record StudentResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        Integer age
) {}
