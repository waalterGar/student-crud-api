package com.students.crud_students.event;

import com.students.crud_students.model.StudentStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

public record StudentDeactivatedEvent(
        String eventId,
        Long studentId,
        StudentStatus status,
        LocalDateTime occurredOn
) implements Serializable {}