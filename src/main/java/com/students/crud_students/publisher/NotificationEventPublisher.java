package com.students.crud_students.publisher;

import com.students.crud_students.event.StudentCreatedEvent;
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
    private String routingKey;

    public void publishStudentCreatedEvent(Long studentId, String firstName, String lastName, String email) {
        StudentCreatedEvent event = new StudentCreatedEvent(
                UUID.randomUUID().toString(),
                studentId,
                firstName,
                lastName,
                email,
                LocalDateTime.now()
        );

        log.info("Publishing StudentCreatedEvent with ID {} to exchange {}", event.eventId(), exchangeName);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
    }
}