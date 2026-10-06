package com.students.crud_students.dto;

import com.students.crud_students.model.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long id,
        String message,
        NotificationType type,
        boolean read,
        Long studentId,
        LocalDateTime createdAt
) {}