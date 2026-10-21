package com.students.crud_students.repository;

import com.students.crud_students.model.Enrollment;
import com.students.crud_students.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, EnrollmentStatus status);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    List<Enrollment> findByStatus(EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course WHERE e.student.id = :studentId")
    List<Enrollment> findAllByStudentIdWithCourse(@Param("studentId") Long studentId);

    @Modifying
    @Query("UPDATE Enrollment e SET e.status = 'CANCELLED' WHERE e.course.id = :courseId AND e.status = 'ACTIVE'")
    int cancelAllActiveEnrollmentsByCourseId(@Param("courseId") Long courseId);
}