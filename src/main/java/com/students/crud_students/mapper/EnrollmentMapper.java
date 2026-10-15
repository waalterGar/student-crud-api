package com.students.crud_students.mapper;

import com.students.crud_students.dto.EnrollmentResponseDTO;
import com.students.crud_students.model.Course;
import com.students.crud_students.model.Enrollment;
import com.students.crud_students.model.EnrollmentStatus;
import com.students.crud_students.model.Student;

public class EnrollmentMapper {

    public static EnrollmentResponseDTO toResponseDto(Enrollment enrollment) {
        String studentName = enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName();

        return new EnrollmentResponseDTO(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                studentName,
                enrollment.getCourse().getId(),
                enrollment.getCourse().getCode(),
                enrollment.getCourse().getTitle(),
                enrollment.getStatus(),
                enrollment.getGrade(),
                enrollment.getEnrolledAt()
        );
    }

    public static Enrollment toEntity(Student student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollment;
    }
}