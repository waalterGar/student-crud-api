package com.students.crud_students.mapper;

import com.students.crud_students.dto.CreateCourseDTO;
import com.students.crud_students.dto.CourseResponseDTO;
import com.students.crud_students.model.Course;
import com.students.crud_students.model.CourseStatus;

public class CourseMapper {

    public static CourseResponseDTO toResponseDto(Course course) {
        return new CourseResponseDTO(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getCredits(),
                course.getMaxCapacity(),
                course.getCurrentEnrolled(),
                course.getStatus(),
                course.getCreatedAt()
        );
    }

    public static Course toEntity(CreateCourseDTO dto) {
        Course course = new Course();
        course.setCode(dto.code());
        course.setTitle(dto.title());
        course.setCredits(dto.credits());
        course.setMaxCapacity(dto.maxCapacity());
        course.setCurrentEnrolled(0);
        course.setStatus(CourseStatus.ACTIVE);
        return course;
    }
}