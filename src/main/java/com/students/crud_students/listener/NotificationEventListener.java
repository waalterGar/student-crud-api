package com.students.crud_students.listener;

import com.students.crud_students.event.StudentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_KEY_PREFIX = "processed_event:";

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void handleStudentCreated(StudentCreatedEvent event) {
        String redisKey = IDEMPOTENCY_KEY_PREFIX + event.eventId();

        // Operación atómica SETNX en Redis: Guarda la clave solo si NO existe (TTL: 24h)
        Boolean isNewEvent = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNewEvent)) {
            log.warn("Event with ID {} has already been processed. Skipping notification.", event.eventId());
            return;
        }

        log.info("Processing notification for student: {} {} (Email: {}) [Event ID: {}]",
                event.firstName(), event.lastName(), event.email(), event.eventId());

        // Simulación de envío de correo académico
        log.info("Notification email successfully dispatched to {}", event.email());
    }
}