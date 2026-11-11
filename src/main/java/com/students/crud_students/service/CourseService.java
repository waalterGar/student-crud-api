package com.students.crud_students.service;

import com.students.crud_students.dto.CreateCourseDTO;
import com.students.crud_students.dto.CourseResponseDTO;
import com.students.crud_students.dto.PageResponse;
import com.students.crud_students.model.CourseStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseService {

    CourseResponseDTO createCourse(CreateCourseDTO createCourseDTO);

    CourseResponseDTO getCourseById(Long id);

    CourseResponseDTO getCourseByCode(String code);

    PageResponse<CourseResponseDTO> getCourses(CourseStatus status, Pageable pageable);

    CourseResponseDTO updateCourseStatus(Long id, CourseStatus status);

    @Caching(evict = {
            @CacheEvict(value = "course_detail", key = "#courseId"),
            @CacheEvict(value = "courses", allEntries = true),
            @CacheEvict(value = "courses_by_status", allEntries = true),
            @CacheEvict(value = "course_detail_code", allEntries = true)
    })
    void evictCourse(Long courseId);
}