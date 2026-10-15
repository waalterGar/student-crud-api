package com.students.crud_students.listener;

import com.students.crud_students.event.StudentEnrolledEvent;
import com.students.crud_students.model.Notification;
import com.students.crud_students.model.NotificationType;
import com.students.crud_students.model.Student;
import com.students.crud_students.repository.NotificationRepository;
import com.students.crud_students.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentEventListener {

    private final StringRedisTemplate redisTemplate;
    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;

    private static final String IDEMPOTENCY_KEY_PREFIX = "processed_enrollment_event:";

    @RabbitListener(queues = "${app.rabbitmq.enrollment-queue:student.enrollment.queue}")
    @Transactional
    @CacheEvict(value = "student_notifications", allEntries = true)
    public void handleStudentEnrolled(StudentEnrolledEvent event) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + event.enrollmentId();

        Boolean isNewEvent = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNewEvent)) {
            log.warn("Enrollment Event for enrollment ID {} has already been processed. Skipping.", event.enrollmentId());
            return;
        }

        Student student = studentRepository.findById(event.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + event.studentId()));

        log.info("Creating enrollment notification for student ID: {} [Enrollment ID: {}]", 
                event.studentId(), event.enrollmentId());

        String message = String.format("You have successfully enrolled in course: %s (%s)", 
                event.courseTitle(), event.courseCode());

        Notification notification = Notification.builder()
                .message(message)
                .type(NotificationType.COURSE_ENROLLED)
                .read(false)
                .student(student)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        log.info("Enrollment notification successfully saved in DB and cache evicted for student ID: {}", event.studentId());
    }
}