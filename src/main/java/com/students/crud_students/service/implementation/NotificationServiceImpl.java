package com.students.crud_students.service.implementation;

import com.students.crud_students.dto.NotificationResponseDTO;
import com.students.crud_students.mapper.NotificationMapper;
import com.students.crud_students.model.Notification;
import com.students.crud_students.repository.NotificationRepository;
import com.students.crud_students.repository.StudentRepository;
import com.students.crud_students.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> getNotificationsByStudent(Long studentId, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Student not found with ID: " + studentId);
        }
        return notificationRepository.findByStudentId(studentId, pageable)
                .map(NotificationMapper::toResponseDto);
    }

    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        notification.setRead(true);
        Notification updatedNotification = notificationRepository.save(notification);

        return NotificationMapper.toResponseDto(updatedNotification);
    }
}