package com.students.crud_students.dto;

public record NotificationBatchResponseDTO(
    Long studentId,
    int updatedCount,
    String message
) {}