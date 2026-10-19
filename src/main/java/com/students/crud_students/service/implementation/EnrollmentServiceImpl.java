package com.students.crud_students.service.implementation;

import com.students.crud_students.dto.EnrollmentRequestDTO;
import com.students.crud_students.dto.EnrollmentResponseDTO;
import com.students.crud_students.dto.UpdateGradeDTO;
import com.students.crud_students.event.StudentEnrolledEvent;
import com.students.crud_students.event.StudentGradedEvent;
import com.students.crud_students.event.StudentUnenrolledEvent;
import com.students.crud_students.mapper.EnrollmentMapper;
import com.students.crud_students.model.*;
import com.students.crud_students.publisher.NotificationEventPublisher;
import com.students.crud_students.repository.CourseRepository;
import com.students.crud_students.repository.EnrollmentRepository;
import com.students.crud_students.repository.StudentRepository;
import com.students.crud_students.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "course_detail", key = "#dto.courseId()"),
            @CacheEvict(value = "course_detail_code", allEntries = true),
            @CacheEvict(value = "courses", allEntries = true),
            @CacheEvict(value = "courses_by_status", allEntries = true)
    })
    public EnrollmentResponseDTO enrollStudent(EnrollmentRequestDTO dto) {
        Student student = studentRepository.findById(dto.studentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + dto.studentId()));

        Course course = courseRepository.findById(dto.courseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + dto.courseId()));

        if (course.getStatus() != CourseStatus.ACTIVE) {
            throw new IllegalStateException("Cannot enroll in a course that is not ACTIVE");
        }

        if (course.getCurrentEnrolled() >= course.getMaxCapacity()) {
            throw new IllegalStateException("Course has reached its maximum capacity (" + course.getMaxCapacity() + ")");
        }

        Optional<Enrollment> existingEnrollmentOpt = enrollmentRepository.findByStudentIdAndCourseId(
                dto.studentId(),
                dto.courseId()
        );

        Enrollment enrollmentToSave;

        if (existingEnrollmentOpt.isPresent()) {
            Enrollment existingEnrollment = existingEnrollmentOpt.get();

            if (existingEnrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                throw new IllegalStateException("Student already has an ACTIVE enrollment in this course");
            }
            existingEnrollment.setStatus(EnrollmentStatus.ACTIVE);
            existingEnrollment.setGrade(null);
            existingEnrollment.setEnrolledAt(LocalDateTime.now());
            enrollmentToSave = existingEnrollment;

        } else {
            // Si no existe registro previo, se crea una entidad nueva
            enrollmentToSave = EnrollmentMapper.toEntity(student, course);
        }
        course.setCurrentEnrolled(course.getCurrentEnrolled() + 1);
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

        notificationEventPublisher.publishStudentEnrolledEvent(event);

        return EnrollmentMapper.toResponseDto(savedEnrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponseDTO getEnrollmentById(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with id: " + id));
        return EnrollmentMapper.toResponseDto(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Student not found with id: " + studentId);
        }
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(EnrollmentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByCourseId(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found with id: " + courseId);
        }
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(EnrollmentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public EnrollmentResponseDTO updateGrade(Long enrollmentId, UpdateGradeDTO dto) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with id: " + enrollmentId));

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot assign grade to a CANCELLED enrollment");
        }

        enrollment.setGrade(dto.grade());

        if (dto.grade().compareTo(new BigDecimal("5.00")) >= 0) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
        } else {
            enrollment.setStatus(EnrollmentStatus.FAILED);
        }

        Enrollment updatedEnrollment = enrollmentRepository.save(enrollment);

        //publicar evento asíncrono a través de NotificationEventPublisher
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
        notificationEventPublisher.publishStudentGradedEvent(studentGradedEvent);


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
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found with id: " + enrollmentId));

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new IllegalStateException("Enrollment is already CANCELLED");
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);

        Course course = enrollment.getCourse();
        if (course.getCurrentEnrolled() > 0) {
            course.setCurrentEnrolled(course.getCurrentEnrolled() - 1);
            courseRepository.save(course);
        }
        Enrollment updatedEnrollment = enrollmentRepository.save(enrollment);
        Student student = updatedEnrollment.getStudent();

        // Publicar evento asíncrono a través de NotificationEventPublisher
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
        notificationEventPublisher.publishStudentUnenrolledEvent(event);

        return EnrollmentMapper.toResponseDto(updatedEnrollment);
    }
}