package com.students.crud_students.service;

import com.students.crud_students.dto.AcademicHistoryDTO;
import com.students.crud_students.dto.PageResponse;
import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;
import com.students.crud_students.dto.StudentUpdateDTO;
import com.students.crud_students.model.StudentStatus;
import org.springframework.data.domain.Pageable;

public interface StudentService {
    PageResponse<StudentResponseDTO> getAllStudents(Pageable pageable);
    StudentResponseDTO getStudentById(Long id);
    StudentResponseDTO createStudent(StudentRequestDTO requestDTO);
    StudentResponseDTO updateStudent(Long id, StudentUpdateDTO requestDTO);
    void deleteStudent(Long id);

    AcademicHistoryDTO getStudentAcademicHistory(Long id);
    StudentResponseDTO changeStatus(Long id, StudentStatus newStatus);
}