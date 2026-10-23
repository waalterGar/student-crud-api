package com.students.crud_students.service.implementation;

import com.students.crud_students.dto.AcademicHistoryDTO;
import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;
import com.students.crud_students.dto.StudentUpdateDTO;
import com.students.crud_students.event.StudentDeactivatedEvent;
import com.students.crud_students.exception.ResourceNotFoundException;
import com.students.crud_students.mapper.EnrollmentMapper;
import com.students.crud_students.mapper.StudentMapper;
import com.students.crud_students.model.Enrollment;
import com.students.crud_students.model.EnrollmentStatus;
import com.students.crud_students.model.Student;
import com.students.crud_students.model.StudentStatus;
import com.students.crud_students.publisher.NotificationEventPublisher;
import com.students.crud_students.repository.EnrollmentRepository;
import com.students.crud_students.repository.StudentRepository;
import com.students.crud_students.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(StudentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "students", key = "#id")
    public StudentResponseDTO getStudentById(Long id) {
        log.info("Fetching student with ID: {} from PostgreSQL", id);
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return StudentMapper.toResponseDto(student);
    }

    @Override
    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO requestDTO) {
        if (studentRepository.existsByEmail(requestDTO.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Student student = StudentMapper.toEntity(requestDTO);
        Student savedStudent = studentRepository.save(student);

        eventPublisher.publishStudentCreatedEvent(
                savedStudent.getId(),
                savedStudent.getFirstName(),
                savedStudent.getLastName(),
                savedStudent.getEmail()
        );

        return StudentMapper.toResponseDto(savedStudent);
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public StudentResponseDTO updateStudent(Long id, StudentUpdateDTO requestDTO) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        log.info("Updating student ID: {} in PostgreSQL", id);

        student.setFirstName(requestDTO.getFirstName());
        student.setLastName(requestDTO.getLastName());
        student.setEmail(requestDTO.getEmail());
        student.setAge(requestDTO.getAge());

        Student updatedStudent = studentRepository.save(student);

        log.info("Student ID: {} updated in DB. Cache evicted for key: students::{}", id, id);
        return StudentMapper.toResponseDto(updatedStudent);
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public void deleteStudent(Long id) {
        log.info("Deleting student ID: {} from PostgreSQL", id);

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        StudentStatus newStatus = StudentStatus.INACTIVE;
        student.setStatus(newStatus);
        studentRepository.save(student);

        eventPublisher.publishStudentDeactivatedEvent(new StudentDeactivatedEvent(student.getId(), newStatus));

        log.info("Student ID: {} deleted from DB. Cache evicted for key: students::{}", id, id);
    }

    @Override
    public AcademicHistoryDTO getStudentAcademicHistory(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        List<Enrollment> enrollments = enrollmentRepository.findAllByStudentIdWithCourse(studentId);

        return new AcademicHistoryDTO(
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getEmail(),
                enrollments.size(),
                countByStatus(enrollments, EnrollmentStatus.ACTIVE),
                countByStatus(enrollments, EnrollmentStatus.COMPLETED),
                countByStatus(enrollments, EnrollmentStatus.FAILED),
                countByStatus(enrollments, EnrollmentStatus.CANCELLED),
                calculateAverageGrade(enrollments),
                enrollments.stream().map(EnrollmentMapper::toProgressDto).toList()
        );

    }

    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public StudentResponseDTO changeStatus(Long id, StudentStatus newStatus) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        if (student.getStatus() == StudentStatus.INACTIVE) {
            throw new IllegalStateException("Cannot change status for an INACTIVE (deleted) student with ID: " + id);
        }

        log.info("Toggling status for student ID: {} from {} to {}", id, student.getStatus(), newStatus);
        student.setStatus(newStatus);

        Student updatedStudent = studentRepository.save(student);

        if (newStatus == StudentStatus.INACTIVE || newStatus == StudentStatus.SUSPENDED) {
            eventPublisher.publishStudentDeactivatedEvent(
                    new StudentDeactivatedEvent(student.getId(), newStatus)
            );
        }

        return StudentMapper.toResponseDto(updatedStudent);
    }

    private int countByStatus(List<Enrollment> enrollments, EnrollmentStatus status) {
        return (int) enrollments.stream()
                .filter(e -> e.getStatus() == status)
                .count();
    }

    private BigDecimal calculateAverageGrade(List<Enrollment> enrollments) {
        List<BigDecimal> grades = enrollments.stream()
                .map(Enrollment::getGrade)
                .filter(Objects::nonNull)
                .toList();

        if (grades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = grades.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(grades.size()), 2, RoundingMode.HALF_UP);
    }
}