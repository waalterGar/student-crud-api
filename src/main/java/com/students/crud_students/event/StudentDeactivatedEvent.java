package com.students.crud_students.event;

import com.students.crud_students.model.StudentStatus;

public record StudentDeactivatedEvent(
        Long studentId,
        StudentStatus status
) {}