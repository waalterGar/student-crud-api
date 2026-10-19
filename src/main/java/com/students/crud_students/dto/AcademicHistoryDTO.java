package com.students.crud_students.dto;

import java.math.BigDecimal;
import java.util.List;

public record AcademicHistoryDTO(
        Long studentId,
        String studentName,
        String email,
        int totalEnrolledCourses,
        int activeCourses,
        int completedCourses,
        int failedCourses,
        int cancelledCourses,
        BigDecimal averageGrade,
        List<CourseProgressDTO> courses
) {}