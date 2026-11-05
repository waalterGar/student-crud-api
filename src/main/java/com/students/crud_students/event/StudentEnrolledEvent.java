package com.students.crud_students.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record StudentEnrolledEvent(
        String eventId,
        Long enrollmentId,
        Long studentId,
        String studentName,
        String studentEmail,
        Long courseId,
        String courseCode,
        String courseTitle,
        LocalDateTime enrolledAt,
        LocalDateTime occurredOn
) implements Serializable {
}