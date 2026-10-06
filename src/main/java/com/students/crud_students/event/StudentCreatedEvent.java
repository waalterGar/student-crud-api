package com.students.crud_students.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record StudentCreatedEvent(
        String eventId,
        Long studentId,
        String firstName,
        String lastName,
        String email,
        LocalDateTime occurredOn
) implements Serializable {}