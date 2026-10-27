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
public class StudentEventRelay {

    private final NotificationEventPublisher notificationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentCreated(StudentCreatedEvent event) {
        log.info("DB transaction committed. Relaying StudentCreatedEvent for student ID: {} to RabbitMQ", event.studentId());
        notificationEventPublisher.publishStudentCreatedEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentDeactivated(StudentDeactivatedEvent event) {
        log.info("DB transaction committed. Relaying StudentDeactivatedEvent for student ID: {} to RabbitMQ", event.studentId());
        notificationEventPublisher.publishStudentDeactivatedEvent(event);
    }
}