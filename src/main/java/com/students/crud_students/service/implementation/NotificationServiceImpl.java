package com.students.crud_students.service.implementation;

import com.students.crud_students.dto.NotificationBatchResponseDTO;
import com.students.crud_students.dto.NotificationResponseDTO;
import com.students.crud_students.dto.PageResponse;
import com.students.crud_students.mapper.NotificationMapper;
import com.students.crud_students.model.Notification;
import com.students.crud_students.repository.NotificationRepository;
import com.students.crud_students.repository.StudentRepository;
import com.students.crud_students.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "student_notifications", key = "#studentId")
    public PageResponse<NotificationResponseDTO> getNotificationsByStudent(Long studentId, Pageable pageable) {
        log.info("Fetching notifications from PostgreSQL for student ID: {}, page: {}",
                studentId, pageable.getPageNumber());

        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Student not found with ID: " + studentId);
        }

        Page<NotificationResponseDTO> page = notificationRepository.findByStudentId(studentId, pageable)
                .map(NotificationMapper::toResponseDto);

        return PageResponse.from(page);
    }

    @Override
    @Transactional
    @CacheEvict(value = "student_notifications", key = "#result.studentId()")
    public NotificationResponseDTO markAsRead(Long notificationId) {
        log.info("Marking notification ID: {} as read", notificationId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        notification.setRead(true);
        Notification updatedNotification = notificationRepository.save(notification);

        log.info("Notification ID: {} updated. Cache evicted for student_notifications", notificationId);

        return NotificationMapper.toResponseDto(updatedNotification);
    }

    @Override
    @Transactional
    @CacheEvict(value = "notifications", key = "#studentId")
    public NotificationBatchResponseDTO markAllAsRead(Long studentId) {
        log.info("Marking all unread notifications as read for student ID: {}", studentId);
        int updatedCount = notificationRepository.markAllAsReadByStudentId(studentId);
        log.info("Updated {} notifications to read status for student ID: {}", updatedCount, studentId);

        return new NotificationBatchResponseDTO(
                studentId,
                updatedCount,
                String.format("Successfully marked %d notifications as read", updatedCount)
        );
    }
}