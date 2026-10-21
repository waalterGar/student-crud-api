package com.students.crud_students.event;

import java.time.LocalDateTime;

public record CourseCancelledEvent(
    Long courseId,
    String courseCode,
    String courseTitle,
    LocalDateTime occurredOn
) {}