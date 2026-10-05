package com.students.crud_students.service;

import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;

import java.util.List;

public interface StudentService {
    List<StudentResponseDTO> getAllStudents();
    StudentResponseDTO getStudentById(Long id);
    StudentResponseDTO createStudent(StudentRequestDTO requestDTO);
    StudentResponseDTO updateStudent(Long id, StudentRequestDTO requestDTO);
    void deleteStudent(Long id);
}