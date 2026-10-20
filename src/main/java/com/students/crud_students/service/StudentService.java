package com.students.crud_students.service;

import com.students.crud_students.dto.AcademicHistoryDTO;
import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;
import com.students.crud_students.dto.StudentUpdateDTO;
import com.students.crud_students.model.StudentStatus;

import java.util.List;

public interface StudentService {
    List<StudentResponseDTO> getAllStudents();
    StudentResponseDTO getStudentById(Long id);
    StudentResponseDTO createStudent(StudentRequestDTO requestDTO);
    StudentResponseDTO updateStudent(Long id, StudentUpdateDTO requestDTO);
    void deleteStudent(Long id);

    AcademicHistoryDTO getStudentAcademicHistory(Long id);
    StudentResponseDTO changeStatus(Long id, StudentStatus newStatus);
}