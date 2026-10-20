package com.students.crud_students.service;

import com.students.crud_students.dto.NotificationBatchResponseDTO;
import com.students.crud_students.dto.NotificationResponseDTO;
import com.students.crud_students.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    PageResponse<NotificationResponseDTO> getNotificationsByStudent(Long studentId, Pageable pageable);

    NotificationResponseDTO markAsRead(Long notificationId);

    NotificationBatchResponseDTO markAllAsRead(Long id);
}