package com.students.crud_students.listener;

import com.students.crud_students.event.CourseCancelledEvent;
import com.students.crud_students.event.StudentDeactivatedEvent;
import com.students.crud_students.event.StudentEnrolledEvent;
import com.students.crud_students.event.StudentGradedEvent;
import com.students.crud_students.event.StudentUnenrolledEvent;
import com.students.crud_students.model.Notification;
import com.students.crud_students.model.NotificationType;
import com.students.crud_students.model.Student;
import com.students.crud_students.repository.EnrollmentRepository;
import com.students.crud_students.repository.NotificationRepository;
import com.students.crud_students.repository.StudentRepository;
import com.students.crud_students.service.CourseService;
import com.students.crud_students.service.EnrollmentService;
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
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@RabbitListener(queues = "${app.rabbitmq.enrollment-queue:student.enrollment.queue}")
public class EnrollmentEventListener {

    private final StringRedisTemplate redisTemplate;
    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentService enrollmentService;
    private final CourseService courseService;
    private final CacheManager cacheManager;

    // Prefijos diferenciados para evitar colisiones en Redis
    private static final String ENROLLMENT_IDEMPOTENCY_PREFIX = "processed_enrollment_event:";
    private static final String UNENROLLMENT_IDEMPOTENCY_PREFIX = "processed_unenrollment_event:";
    private static final String GRADING_IDEMPOTENCY_PREFIX = "processed_grading_event:";

    @RabbitHandler
    @Transactional
    @CacheEvict(value = "student_notifications", key = "#event.studentId()")
    public void handleStudentEnrolled(StudentEnrolledEvent event) {
        String redisKey = ENROLLMENT_IDEMPOTENCY_PREFIX + event.eventId();
        String message = String.format("You have successfully enrolled in course: %s (%s)",
                event.courseTitle(), event.courseCode());

        processNotification(redisKey, event.eventId(), event.studentId(), NotificationType.COURSE_ENROLLED, message);
    }

    @RabbitHandler
    @Transactional
    @CacheEvict(value = "student_notifications", key = "#event.studentId()")
    public void handleStudentUnenrolled(StudentUnenrolledEvent event) {
        String redisKey = UNENROLLMENT_IDEMPOTENCY_PREFIX + event.eventId();
        String message = String.format("You have successfully unenrolled from course: %s (%s)",
                event.courseTitle(), event.courseCode());

        processNotification(redisKey, event.eventId(), event.studentId(), NotificationType.COURSE_UNENROLLED, message);
    }

    @RabbitHandler
    @Transactional
    @CacheEvict(value = "student_notifications", key = "#event.studentId()")
    public void handleStudentGraded(StudentGradedEvent event) {
        String redisKey = GRADING_IDEMPOTENCY_PREFIX + event.eventId();
        String message = String.format(Locale.ROOT, "Your grade for course %s (%s) has been updated to: %.2f",
                event.courseTitle(), event.courseCode(), event.grade());

        processNotification(redisKey, event.eventId(), event.studentId(), NotificationType.GRADE_PUBLISHED, message);
    }

    @RabbitHandler
    public void handleCourseCancelled(CourseCancelledEvent event) {
        int studentCount = (event.affectedStudentIds() != null) ? event.affectedStudentIds().size() : 0;
        log.info("Received CourseCancelledEvent for course ID: {} [Event ID: {}]. Dispatching notifications to {} students.",
                event.courseId(), event.eventId(), studentCount);

        if (event.affectedStudentIds() == null || event.affectedStudentIds().isEmpty()) {
            return;
        }

        String message = String.format("Your enrollment in course %s (%s) was cancelled because the course was cancelled by administration.",
                event.courseTitle(), event.courseCode());

        for (Long studentId : event.affectedStudentIds()) {
            studentRepository.findById(studentId).ifPresent(student -> {
                Notification notification = Notification.builder()
                        .message(message)
                        .type(NotificationType.COURSE_UNENROLLED)
                        .read(false)
                        .student(student)
                        .createdAt(LocalDateTime.now())
                        .build();

                notificationRepository.save(notification);


                var cache = cacheManager.getCache("student_notifications");
                if (cache != null) {
                    cache.evict(studentId);
                }
            });
        }

        log.info("Successfully created notifications and evicted cache for {} students of course ID: {}",
                studentCount, event.courseId());
    }

    @RabbitHandler
    public void handleStudentDeactivated(StudentDeactivatedEvent event) {
        log.info("Received StudentDeactivatedEvent for student ID: {}", event.studentId());

        Set<Long> affected = enrollmentService.cancelAllByStudent(event.studentId());

        affected.forEach(courseService::evictCourse);
    }

    private void processNotification(String redisKey, String eventId, Long studentId, NotificationType type, String message) {
        Boolean isNewEvent = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNewEvent)) {
            log.warn("Event ID {} has already been processed. Skipping.", eventId);
            return;
        }

        try {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

            log.info("Creating {} notification for student ID: {} [Event ID: {}]", type, studentId, eventId);

            Notification notification = Notification.builder()
                    .message(message)
                    .type(type)
                    .read(false)
                    .student(student)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            log.info("{} notification successfully saved in DB and cache evicted for student ID: {}", type, studentId);
        } catch (Exception ex) {
            redisTemplate.delete(redisKey);
            log.error("Failed to process notification for event ID {}. Redis key released to allow retry/DLQ.", eventId, ex);
            throw ex;
        }
    }
}