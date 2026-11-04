package com.students.crud_students.service.implementation;

import com.students.crud_students.dto.EnrollmentRequestDTO;
import com.students.crud_students.dto.EnrollmentResponseDTO;
import com.students.crud_students.dto.UpdateGradeDTO;
import com.students.crud_students.event.StudentEnrolledEvent;
import com.students.crud_students.event.StudentGradedEvent;
import com.students.crud_students.event.StudentUnenrolledEvent;
import com.students.crud_students.exception.BusinessRuleViolationException;
import com.students.crud_students.exception.DuplicateResourceException;
import com.students.crud_students.exception.InvalidStateTransitionException;
import com.students.crud_students.exception.ResourceNotFoundException;
import com.students.crud_students.mapper.EnrollmentMapper;
import com.students.crud_students.model.*;
import com.students.crud_students.repository.CourseRepository;
import com.students.crud_students.repository.EnrollmentRepository;
import com.students.crud_students.repository.StudentRepository;
import com.students.crud_students.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "course_detail", key = "#dto.courseId()"),
            @CacheEvict(value = "course_detail_code", allEntries = true),
            @CacheEvict(value = "courses", allEntries = true),
            @CacheEvict(value = "courses_by_status", allEntries = true)
    })
    public EnrollmentResponseDTO enrollStudent(EnrollmentRequestDTO dto) {
        Student student = studentRepository.findByIdForShare(dto.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + dto.studentId()));

        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Student is not active. " +
                    "Current status: " + student.getStatus());
        }

        Course course = courseRepository.findByIdForUpdate(dto.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + dto.courseId()));

        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Course is not active. Current status: " + course.getStatus());
        }

        if (course.getCurrentEnrolled() >= course.getMaxCapacity()) {
            throw new BusinessRuleViolationException("Course is full. Capacity: " + course.getMaxCapacity());
        }

        if (enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(dto.studentId(), dto.courseId(), EnrollmentStatus.ACTIVE)) {
            throw new DuplicateResourceException("Student is already actively enrolled in this course");
        }

        Enrollment  enrollmentToSave = EnrollmentMapper.toEntity(student, course);

        course.setCurrentEnrolled(course.getCurrentEnrolled() + 1);

        if (course.getCurrentEnrolled().equals(course.getMaxCapacity())) {
            log.info("Course ID {} reached max capacity. Transitioning status to CLOSED.", course.getId());
            course.setStatus(CourseStatus.CLOSED);
        }

        courseRepository.save(course);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollmentToSave);

        StudentEnrolledEvent event = new StudentEnrolledEvent(
                savedEnrollment.getId(),
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getEmail(),
                course.getId(),
                course.getCode(),
                course.getTitle(),
                savedEnrollment.getEnrolledAt()
        );

        eventPublisher.publishEvent(event);

        return EnrollmentMapper.toResponseDto(savedEnrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponseDTO getEnrollmentById(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
        return EnrollmentMapper.toResponseDto(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(EnrollmentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByCourseId(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(EnrollmentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "course_detail", key = "#result.courseId"),
            @CacheEvict(value = "course_detail_code", allEntries = true),
            @CacheEvict(value = "courses", allEntries = true),
            @CacheEvict(value = "courses_by_status", allEntries = true)
    })
    public EnrollmentResponseDTO updateGrade(Long enrollmentId, UpdateGradeDTO dto) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Cannot assign grade to an enrollment that is not ACTIVE. Current status: " + enrollment.getStatus());
        }

        Course course = courseRepository.findByIdForUpdate(enrollment.getCourse().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + enrollment.getCourse().getId()));

        enrollment.setGrade(dto.grade());
        if (dto.grade().compareTo(new BigDecimal("5.00")) >= 0) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
        } else {
            enrollment.setStatus(EnrollmentStatus.FAILED);
        }

        int previousEnrolled = course.getCurrentEnrolled();

        if (course.getCurrentEnrolled() > 0) {
            course.setCurrentEnrolled(course.getCurrentEnrolled() - 1);
        }

        if (course.getStatus() == CourseStatus.CLOSED && previousEnrolled == course.getMaxCapacity()) {
            log.info("Seat freed up in Course ID {} after grading. Transitioning status back to ACTIVE.", course.getId());
            course.setStatus(CourseStatus.ACTIVE);
        }

        courseRepository.save(course);
        Enrollment updatedEnrollment = enrollmentRepository.save(enrollment);

        // 5. Publicar evento
        StudentGradedEvent studentGradedEvent = new StudentGradedEvent(
                updatedEnrollment.getId(),
                updatedEnrollment.getStudent().getId(),
                updatedEnrollment.getStudent().getFirstName() + " " + updatedEnrollment.getStudent().getLastName(),
                updatedEnrollment.getStudent().getEmail(),
                updatedEnrollment.getCourse().getId(),
                updatedEnrollment.getCourse().getCode(),
                updatedEnrollment.getCourse().getTitle(),
                updatedEnrollment.getGrade(),
                updatedEnrollment.getStatus(),
                updatedEnrollment.getEnrolledAt()
        );
        eventPublisher.publishEvent(studentGradedEvent);

        return EnrollmentMapper.toResponseDto(updatedEnrollment);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "course_detail", allEntries = true),
            @CacheEvict(value = "course_detail_code", allEntries = true),
            @CacheEvict(value = "courses", allEntries = true),
            @CacheEvict(value = "courses_by_status", allEntries = true)
    })
    public EnrollmentResponseDTO cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Cannot cancel an enrollment that is not ACTIVE. Current status: " + enrollment.getStatus());
        }

        Course course = courseRepository.findByIdForUpdate(enrollment.getCourse().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + enrollment.getCourse().getId()));

        enrollment.setStatus(EnrollmentStatus.CANCELLED);

        int previousEnrolled = course.getCurrentEnrolled();

        if (course.getCurrentEnrolled() > 0) {
            course.setCurrentEnrolled(course.getCurrentEnrolled() - 1);
        }

        if (course.getStatus() == CourseStatus.CLOSED && previousEnrolled == course.getMaxCapacity()) {
            log.info("Seat freed up in Course ID {}. Transitioning status back to ACTIVE.", course.getId());
            course.setStatus(CourseStatus.ACTIVE);
        }

        courseRepository.save(course);
        Enrollment updatedEnrollment = enrollmentRepository.save(enrollment);

        // 5. Publicación de evento
        Student student = updatedEnrollment.getStudent();
        StudentUnenrolledEvent event = new StudentUnenrolledEvent(
                updatedEnrollment.getId(),
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getEmail(),
                course.getId(),
                course.getCode(),
                course.getTitle(),
                updatedEnrollment.getEnrolledAt()
        );
        eventPublisher.publishEvent(event);

        return EnrollmentMapper.toResponseDto(updatedEnrollment);
    }

    @Transactional
    @Override
    public Set<Long> cancelAllByStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if (student.getStatus() == StudentStatus.ACTIVE) {
            log.warn("Skipping cascade cancellation for student ID: {}. Student is currently ACTIVE.", studentId);
            return java.util.Collections.emptySet();
        }

        List<Enrollment> active = enrollmentRepository
                .findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE);

        Set<Long> courseIds = active.stream()
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toSet());

        active.forEach(e -> e.setStatus(EnrollmentStatus.CANCELLED));

        for (Long courseId : courseIds) {
            Course course = courseRepository.findByIdForUpdate(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

            int previousEnrolled = course.getCurrentEnrolled();

            if (course.getCurrentEnrolled() > 0) {
                course.setCurrentEnrolled(course.getCurrentEnrolled() - 1);
            }

            if (course.getStatus() == CourseStatus.CLOSED && previousEnrolled == course.getMaxCapacity()) {
                course.setStatus(CourseStatus.ACTIVE);
            }
            courseRepository.save(course);
        }
        return courseIds;
    }
}