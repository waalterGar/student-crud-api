package com.students.crud_students.listener;


import com.students.crud_students.event.StudentCreatedEvent;
import com.students.crud_students.model.Notification;
import com.students.crud_students.model.NotificationType;
import com.students.crud_students.model.Student;
import com.students.crud_students.repository.NotificationRepository;
import com.students.crud_students.repository.StudentRepository;
import com.students.crud_students.service.EmailService;
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
public class EmailEventListener {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    private static final String IDEMPOTENCY_KEY_PREFIX = "processed_event:email:";

    @RabbitListener(queues = "${rabbitmq.queue.email.name:student.email.queue}")
    public void handleStudentCreated(StudentCreatedEvent event) {
        log.info("Received event for email dispatch: {}", event.eventId());

        String redisKey = IDEMPOTENCY_KEY_PREFIX + event.eventId();

        Boolean isNewEvent = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNewEvent)) {
            log.warn("Event with ID {} has already been processed. Skipping email.", event.eventId());
            return;
        }

        String fullName = event.firstName() + " " + event.lastName();
        emailService.sendWelcomeEmail(event.email(), fullName);

        log.info("Sent welcome email to student: {} ({}) for event ID: {}", fullName, event.email(), event.eventId());
    }
}
