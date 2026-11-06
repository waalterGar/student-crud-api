package com.students.crud_students.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CourseCancelledEvent(
        String eventId,
        Long courseId,
        String courseCode,
        String courseTitle,
        LocalDateTime occurredOn
) implements Serializable {
}