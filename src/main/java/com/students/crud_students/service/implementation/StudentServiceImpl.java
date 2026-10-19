package com.students.crud_students.service.implementation;

import com.students.crud_students.dto.AcademicHistoryDTO;
import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;
import com.students.crud_students.exception.ResourceNotFoundException;
import com.students.crud_students.mapper.EnrollmentMapper;
import com.students.crud_students.mapper.StudentMapper;
import com.students.crud_students.model.Enrollment;
import com.students.crud_students.model.EnrollmentStatus;
import com.students.crud_students.model.Student;
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

        // Publicación asíncrona del evento hacia RabbitMQ
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
    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO requestDTO) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        log.info("Updating student ID: {} in PostgreSQL", id);

        student.setFirstName(requestDTO.firstName());
        student.setLastName(requestDTO.lastName());
        student.setEmail(requestDTO.email());
        student.setAge(requestDTO.age());

        Student updatedStudent = studentRepository.save(student);

        log.info("Student ID: {} updated in DB. Cache evicted for key: students::{}", id, id);
        return StudentMapper.toResponseDto(updatedStudent);
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", key = "#id")
    public void deleteStudent(Long id) {
        log.info("Deleting student ID: {} from PostgreSQL", id);

        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);

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