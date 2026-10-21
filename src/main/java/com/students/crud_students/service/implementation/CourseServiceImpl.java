package com.students.crud_students.service.implementation;

import com.students.crud_students.dto.CreateCourseDTO;
import com.students.crud_students.dto.CourseResponseDTO;
import com.students.crud_students.event.CourseCancelledEvent;
import com.students.crud_students.mapper.CourseMapper;
import com.students.crud_students.model.Course;
import com.students.crud_students.model.CourseStatus;
import com.students.crud_students.model.Enrollment;
import com.students.crud_students.publisher.NotificationEventPublisher;
import com.students.crud_students.repository.CourseRepository;
import com.students.crud_students.repository.EnrollmentRepository;
import com.students.crud_students.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "courses", allEntries = true),
            @CacheEvict(value = "courses_by_status", allEntries = true)
    })
    public CourseResponseDTO createCourse(CreateCourseDTO dto) {
        if (courseRepository.existsByCode(dto.code())) {
            throw new IllegalArgumentException("Course code already exists: " + dto.code());
        }

        Course course = CourseMapper.toEntity(dto);
        Course savedCourse = courseRepository.save(course);
        return CourseMapper.toResponseDto(savedCourse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "course_detail", key = "#id")
    public CourseResponseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + id));
        return CourseMapper.toResponseDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "course_detail_code", key = "#code")
    public CourseResponseDTO getCourseByCode(String code) {
        Course course = courseRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with code: " + code));
        return CourseMapper.toResponseDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "courses")
    public List<CourseResponseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(CourseMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "courses_by_status", key = "#status.name()")
    public List<CourseResponseDTO> getCoursesByStatus(CourseStatus status) {
        return courseRepository.findByStatus(status).stream()
                .map(CourseMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "course_detail", key = "#id"),
            @CacheEvict(value = "course_detail_code", allEntries = true),
            @CacheEvict(value = "courses", allEntries = true),
            @CacheEvict(value = "courses_by_status", allEntries = true)
    })
    public CourseResponseDTO updateCourseStatus(Long id, CourseStatus status) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + id));

        course.setStatus(status);

        if (status == CourseStatus.CANCELLED) {
            log.warn("Course ID {} was CANCELLED. Resetting capacity and emitting CourseCancelledEvent.", id);
            course.setCurrentEnrolled(0);

            notificationEventPublisher.publishCourseCancelledEvent(
                    new CourseCancelledEvent(course.getId(), course.getCode(), course.getTitle(), LocalDateTime.now())
            );
        }

        Course updatedCourse = courseRepository.save(course);
        return CourseMapper.toResponseDto(updatedCourse);
    }
}