package com.students.crud_students.service;

import com.students.crud_students.dto.NotificationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    Page<NotificationResponseDTO> getNotificationsByStudent(Long studentId, Pageable pageable);

    NotificationResponseDTO markAsRead(Long notificationId);
}