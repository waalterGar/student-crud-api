package com.students.crud_students.service;

import com.students.crud_students.dto.CreateCourseDTO;
import com.students.crud_students.dto.CourseResponseDTO;
import com.students.crud_students.model.CourseStatus;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

public interface CourseService {

    CourseResponseDTO createCourse(CreateCourseDTO createCourseDTO);

    CourseResponseDTO getCourseById(Long id);

    CourseResponseDTO getCourseByCode(String code);

    List<CourseResponseDTO> getAllCourses();

    List<CourseResponseDTO> getCoursesByStatus(CourseStatus status);

    CourseResponseDTO updateCourseStatus(Long id, CourseStatus status);

    @CacheEvict(value = "courses", key = "#courseId")
    void evictCourse(Long courseId);
}