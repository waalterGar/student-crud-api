package com.students.crud_students.publisher;

import com.students.crud_students.event.StudentCreatedEvent;
import com.students.crud_students.event.StudentEnrolledEvent;
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
}