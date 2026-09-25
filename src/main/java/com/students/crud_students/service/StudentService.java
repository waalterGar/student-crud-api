package com.students.crud_students.service;


import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;
import com.students.crud_students.mapper.StudentMapper;
import com.students.crud_students.model.Student;
import com.students.crud_students.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public List<StudentResponseDTO> getAllStudents() {
        return repository.findAll()
                .stream()
                .map(StudentMapper::toResponseDto)
                .toList();
    }

    public Optional<StudentResponseDTO> getStudentById(Long id) {
        return repository.findById(id)
                .map(StudentMapper::toResponseDto);
    }

    public StudentResponseDTO createStudent(StudentRequestDTO dto) {
        Student student = StudentMapper.toEntity(dto);
        Student savedStudent = repository.save(student);
        return StudentMapper.toResponseDto(savedStudent);
    }

    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO dto) {
        return repository.findById(id).map(student -> {
            student.setFirstName(dto.firstName());
            student.setLastName(dto.lastName());
            student.setEmail(dto.email());
            student.setAge(dto.age());
            Student updated = repository.save(student);
            return StudentMapper.toResponseDto(updated);
        }).orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    public void deleteStudent(Long id) {
        repository.deleteById(id);
    }
}
