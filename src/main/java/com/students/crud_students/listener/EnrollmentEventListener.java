package com.students.crud_students.listener;

import com.students.crud_students.event.*;
import com.students.crud_students.exception.ResourceNotFoundException;
import com.students.crud_students.model.*;
import com.students.crud_students.repository.CourseRepository;
import com.students.crud_students.repository.EnrollmentRepository;
import com.students.crud_students.repository.NotificationRepository;
import com.students.crud_students.repository.StudentRepository;
import com.students.crud_students.service.CourseService;
import com.students.crud_students.service.EnrollmentService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@RabbitListener(queues = "${app.rabbitmq.enrollment-queue:student.enrollment.queue}")
public class EnrollmentEventListener {

    private final StringRedisTemplate redisTemplate;
    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;

    // Prefijos diferenciados para evitar colisiones en Redis
    private static final String ENROLLMENT_IDEMPOTENCY_PREFIX = "processed_enrollment_event:";
    private static final String UNENROLLMENT_IDEMPOTENCY_PREFIX = "processed_unenrollment_event:";
    private static final String GRADING_IDEMPOTENCY_PREFIX = "processed_grading_event:";

    @RabbitHandler
    @Transactional
    @CacheEvict(value = "student_notifications", key = "#event.studentId()")
    public void handleStudentEnrolled(StudentEnrolledEvent event) {
        String redisKey = ENROLLMENT_IDEMPOTENCY_PREFIX + event.enrollmentId();

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

    @RabbitHandler
    @Transactional
    @CacheEvict(value = "student_notifications", key = "#event.studentId()")
    public void handleStudentUnenrolled(StudentUnenrolledEvent event) {
        String redisKey = UNENROLLMENT_IDEMPOTENCY_PREFIX + event.enrollmentId();

        Boolean isNewEvent = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNewEvent)) {
            log.warn("Unenrollment Event for enrollment ID {} has already been processed. Skipping.", event.enrollmentId());
            return;
        }

        Student student = studentRepository.findById(event.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + event.studentId()));

        log.info("Creating Unenrollment notification for student ID: {} [Enrollment ID: {}]",
                event.studentId(), event.enrollmentId());

        String message = String.format("You have successfully unenrolled from course: %s (%s)",
                event.courseTitle(), event.courseCode());

        Notification notification = Notification.builder()
                .message(message)
                .type(NotificationType.COURSE_UNENROLLED)
                .read(false)
                .student(student)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        log.info("Unenrollment notification successfully saved in DB and cache evicted for student ID: {}", event.studentId());
    }

    @RabbitHandler
    @Transactional
    @CacheEvict(value = "student_notifications", key = "#event.studentId()")
    public void handleStudentGraded(StudentGradedEvent event) {
        String redisKey = GRADING_IDEMPOTENCY_PREFIX + event.enrollmentId();

        Boolean isNewEvent = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNewEvent)) {
            log.warn("Grading Event for enrollment ID {} has already been processed. Skipping.", event.enrollmentId());
            return;
        }

        Student student = studentRepository.findById(event.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + event.studentId()));

        log.info("Creating Grading notification for student ID: {} [Enrollment ID: {}]",
                event.studentId(), event.enrollmentId());

        String message = String.format("Your grade for course %s (%s) has been updated to: %.2f",
                event.courseTitle(), event.courseCode(), event.grade());

        Notification notification = Notification.builder()
                .message(message)
                .type(NotificationType.GRADE_PUBLISHED)
                .read(false)
                .student(student)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        log.info("Grading notification successfully saved in DB and cache evicted for student ID: {}", event.studentId());
    }

    @RabbitHandler
    @Transactional
    public void handleCourseCancelled(CourseCancelledEvent event) {
        log.info("Received CourseCancelledEvent for course ID: {}. Processing cascade enrollment cancellation.", event.courseId());

        // Ejecuta la actualización atómica en la base de datos
        int cancelledCount = enrollmentRepository.cancelAllActiveEnrollmentsByCourseId(event.courseId());

        log.info("Successfully cancelled {} active enrollments for course ID: {}", cancelledCount, event.courseId());
    }

    @RabbitHandler
    public void handleStudentDeactivated(StudentDeactivatedEvent event) {
        log.info("Received StudentDeactivatedEvent for student ID: {}", event.studentId());

        Set<Long> affected = enrollmentService.cancelAllByStudent(event.studentId());

        affected.forEach(courseService::evictCourse);
    }
}