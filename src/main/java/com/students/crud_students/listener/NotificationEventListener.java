package com.students.crud_students.listener;

import com.students.crud_students.event.StudentCreatedEvent;
import com.students.crud_students.model.Notification;
import com.students.crud_students.model.NotificationType;
import com.students.crud_students.model.Student;
import com.students.crud_students.repository.NotificationRepository;
import com.students.crud_students.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final StringRedisTemplate redisTemplate;
    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;

    private static final String IDEMPOTENCY_KEY_PREFIX = "processed_event:";

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    @Transactional
    public void handleStudentCreated(StudentCreatedEvent event) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + event.eventId();

        Boolean isNewEvent = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNewEvent)) {
            log.warn("Event with ID {} has already been processed. Skipping notification.", event.eventId());
            return;
        }

        Student student = studentRepository.findById(event.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + event.studentId()));

        Notification notification = Notification.builder()
                .message("Welcome to the platform, " + event.firstName() + "!")
                .type(NotificationType.WELCOME)
                .read(false)
                .student(student)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        log.info("Notification successfully stored in DB for student ID: {} [Event ID: {}]",
                event.studentId(), event.eventId());
    }
}