package com.students.crud_students.mapper;

import com.students.crud_students.dto.NotificationResponseDTO;
import com.students.crud_students.model.Notification;

public class NotificationMapper {

    public static NotificationResponseDTO toResponseDto(Notification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getStudent().getId(),
                notification.getCreatedAt()
        );
    }
}