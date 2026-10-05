package com.students.crud_students.service.implementation;

import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;
import com.students.crud_students.exception.ResourceNotFoundException;
import com.students.crud_students.mapper.StudentMapper;
import com.students.crud_students.model.Student;
import com.students.crud_students.repository.StudentRepository;
import com.students.crud_students.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponseDTO> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(StudentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponseDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return StudentMapper.toResponseDto(student);
    }

    @Override
    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO requestDTO) {
        Student student = StudentMapper.toEntity(requestDTO);
        Student savedStudent = studentRepository.save(student);
        return StudentMapper.toResponseDto(savedStudent);
    }

    @Override
    @Transactional
    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO requestDTO) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        student.setFirstName(requestDTO.firstName());
        student.setLastName(requestDTO.lastName());
        student.setEmail(requestDTO.email());
        student.setAge(requestDTO.age());

        Student updatedStudent = studentRepository.save(student);
        return StudentMapper.toResponseDto(updatedStudent);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }
}