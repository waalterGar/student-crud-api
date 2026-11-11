package com.students.crud_students.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record CourseCancelledEvent(
        String eventId,
        Long courseId,
        String courseCode,
        String courseTitle,
        List<Long> affectedStudentIds,
        LocalDateTime occurredOn
) implements Serializable {
}