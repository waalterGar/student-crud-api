package com.students.crud_students.service;

import com.students.crud_students.dto.EnrollmentRequestDTO;
import com.students.crud_students.dto.EnrollmentResponseDTO;
import com.students.crud_students.dto.UpdateGradeDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface EnrollmentService {

    EnrollmentResponseDTO enrollStudent(EnrollmentRequestDTO requestDTO);

    EnrollmentResponseDTO getEnrollmentById(Long id);

    List<EnrollmentResponseDTO> getEnrollmentsByStudentId(Long studentId);

    List<EnrollmentResponseDTO> getEnrollmentsByCourseId(Long courseId);

    EnrollmentResponseDTO updateGrade(Long enrollmentId, UpdateGradeDTO updateGradeDTO);

    EnrollmentResponseDTO cancelEnrollment(Long enrollmentId);

    @Transactional
    Set<Long> cancelAllByStudent(Long studentId);
}