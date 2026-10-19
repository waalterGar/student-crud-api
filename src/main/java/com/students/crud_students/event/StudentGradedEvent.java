package com.students.crud_students.event;

import com.students.crud_students.model.EnrollmentStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentGradedEvent(
    Long enrollmentId,
    Long studentId,
    String studentName,
    String studentEmail,
    Long courseId,
    String courseCode,
    String courseTitle,
    BigDecimal grade,
    EnrollmentStatus status,
    LocalDateTime enrolledAt
) implements Serializable {}
