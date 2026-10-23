package com.students.crud_students.publisher;

import com.students.crud_students.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-key}")
    private String studentCreatedRoutingKey;

    @Value("${app.rabbitmq.enrollment-routing-key:student.enrollment.created}")
    private String studentEnrolledRoutingKey;

    @Value("${app.rabbitmq.unenrollment-routing-key:student.enrollment.cancelled}")
    private String studentUnenrolledRoutingKey;

    @Value("${app.rabbitmq.grading-routing-key:student.enrollment.graded}")
    private String gradingRoutingKey;

    @Value("${app.rabbitmq.course-cancelled-routing-key:student.course.cancelled}")
    private String courseCancelledRoutingKey;

    @Value("${app.rabbitmq.student-deactivated-routing-key:student.status.deactivated}")
    private String studentDeactivatedRoutingKey;

    public void publishStudentCreatedEvent(Long studentId, String firstName, String lastName, String email) {
        StudentCreatedEvent event = new StudentCreatedEvent(
                UUID.randomUUID().toString(),
                studentId,
                firstName,
                lastName,
                email,
                LocalDateTime.now()
        );

        log.info("Publishing StudentCreatedEvent with ID {} to exchange {} using routing key {}",
                event.eventId(), exchangeName, studentCreatedRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, studentCreatedRoutingKey, event);
    }

    public void publishStudentEnrolledEvent(StudentEnrolledEvent event) {
        log.info("Publishing StudentEnrolledEvent for enrollmentId {} to exchange {} using routing key {}",
                event.enrollmentId(), exchangeName, studentEnrolledRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, studentEnrolledRoutingKey, event);
    }

    public void publishStudentUnenrolledEvent(StudentUnenrolledEvent event) {
        log.info("Publishing StudenUnenrolledEvent for enrollmentId {} to exchange {} using routing key {}",
                event.enrollmentId(), exchangeName, studentUnenrolledRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, studentUnenrolledRoutingKey, event);
    }

    public void publishStudentGradedEvent(StudentGradedEvent event) {
        log.info("Publishing StudentGradedEvent for enrollmentId {} to exchange {} using routing key {}",
                event.enrollmentId(), exchangeName, studentUnenrolledRoutingKey);

        rabbitTemplate.convertAndSend(exchangeName, studentUnenrolledRoutingKey, event);
    }

    public void publishCourseCancelledEvent(CourseCancelledEvent event) {
        log.info("Publishing CourseCancelledEvent for course ID: {} to exchange: {}", event.courseId(), exchangeName);
        rabbitTemplate.convertAndSend(exchangeName, courseCancelledRoutingKey, event);
    }

    public void publishStudentDeactivatedEvent(StudentDeactivatedEvent event) {
        log.info("Publishing StudentDeactivatedEvent for student ID: {} with routing key {}",
                event.studentId(), studentDeactivatedRoutingKey);
        rabbitTemplate.convertAndSend(exchangeName, studentDeactivatedRoutingKey, event);
    }
}