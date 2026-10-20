package com.students.crud_students.mapper;

import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;
import com.students.crud_students.model.Student;
import com.students.crud_students.model.StudentStatus;

public class StudentMapper {

    public static StudentResponseDTO toResponseDto(Student student) {
        return new StudentResponseDTO(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                student.getAge(),
                student.getStatus()
        );
    }

    public static Student toEntity(StudentRequestDTO dto) {
        Student student = new Student();
        student.setFirstName(dto.firstName());
        student.setLastName(dto.lastName());
        student.setEmail(dto.email());
        student.setAge(dto.age());
        student.setStatus(StudentStatus.ACTIVE);

        return student;
    }
}

