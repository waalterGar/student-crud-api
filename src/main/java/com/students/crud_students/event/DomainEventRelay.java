package com.students.crud_students.event;

import com.students.crud_students.publisher.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventRelay {

    private final NotificationEventPublisher notificationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentCreated(StudentCreatedEvent event) {
        log.info("DB transaction committed. Relaying StudentCreatedEvent for student ID: {} to RabbitMQ", event.studentId());
        try {
            notificationEventPublisher.publishStudentCreatedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish StudentCreatedEvent to RabbitMQ for student ID: {}. Error: {}",
                    event.studentId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentDeactivated(StudentDeactivatedEvent event) {
        log.info("DB transaction committed. Relaying StudentDeactivatedEvent for student ID: {} to RabbitMQ", event.studentId());
        try {
            notificationEventPublisher.publishStudentDeactivatedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish StudentDeactivatedEvent to RabbitMQ for student ID: {}. Error: {}",
                    event.studentId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentEnrolledEvent(StudentEnrolledEvent event) {
        log.info("Relaying StudentEnrolledEvent after DB commit for enrollment ID: {}", event.enrollmentId());
        try {
            notificationEventPublisher.publishStudentEnrolledEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish StudentEnrolledEvent to RabbitMQ for enrollment ID: {}. Error: {}",
                    event.enrollmentId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentUnenrolledEvent(StudentUnenrolledEvent event) {
        log.info("Relaying StudentUnenrolledEvent after DB commit for enrollment ID: {}", event.enrollmentId());
        try {
            notificationEventPublisher.publishStudentUnenrolledEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish StudentUnenrolledEvent to RabbitMQ for enrollment ID: {}. Error: {}",
                    event.enrollmentId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentGradedEvent(StudentGradedEvent event) {
        log.info("Relaying StudentGradedEvent after DB commit for enrollment ID: {}", event.enrollmentId());
        try {
            notificationEventPublisher.publishStudentGradedEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish StudentGradedEvent to RabbitMQ for enrollment ID: {}. Error: {}",
                    event.enrollmentId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseCancelledEvent(CourseCancelledEvent event) {
        log.info("Relaying CourseCancelledEvent after DB commit for course ID: {}", event.courseId());
        try {
            notificationEventPublisher.publishCourseCancelledEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish CourseCancelledEvent to RabbitMQ for course ID: {}. Error: {}",
                    event.courseId(), e.getMessage(), e);
        }
    }
}