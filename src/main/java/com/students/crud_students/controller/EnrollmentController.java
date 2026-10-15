package com.students.crud_students.controller;

import com.students.crud_students.dto.EnrollmentRequestDTO;
import com.students.crud_students.dto.EnrollmentResponseDTO;
import com.students.crud_students.dto.UpdateGradeDTO;
import com.students.crud_students.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Endpoints for student course enrollment and grading")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Operation(summary = "Enroll a student in a course",
            description = "Registers an enrollment after verifying capacity limits, duplicate preventions, and course active status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Student enrolled successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EnrollmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @ApiResponse(responseCode = "409", description = "Business conflict (course at capacity, duplicate enrollment, or inactive course)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Student or Course not found", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EnrollmentResponseDTO> enrollStudent(@Valid @RequestBody EnrollmentRequestDTO requestDTO) {
        EnrollmentResponseDTO enrollment = enrollmentService.enrollStudent(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @Operation(summary = "Get enrollment by ID", description = "Retrieves details for a specific enrollment record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enrollment found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EnrollmentResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Enrollment not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponseDTO> getEnrollmentById(
            @Parameter(description = "ID of the enrollment", example = "1") @PathVariable Long id) {
        EnrollmentResponseDTO enrollment = enrollmentService.getEnrollmentById(id);
        return ResponseEntity.ok(enrollment);
    }

    @Operation(summary = "Get enrollments by student", description = "Lists all course enrollments for a specific student.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of enrollments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found", content = @Content)
    })
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<EnrollmentResponseDTO>> getEnrollmentsByStudentId(
            @Parameter(description = "ID of the student", example = "1") @PathVariable Long studentId) {
        List<EnrollmentResponseDTO> enrollments = enrollmentService.getEnrollmentsByStudentId(studentId);
        return ResponseEntity.ok(enrollments);
    }

    @Operation(summary = "Get enrollments by course", description = "Returns the list of enrolled students for a given course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course class roster retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content)
    })
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<EnrollmentResponseDTO>> getEnrollmentsByCourseId(
            @Parameter(description = "ID of the course", example = "1") @PathVariable Long courseId) {
        List<EnrollmentResponseDTO> enrollments = enrollmentService.getEnrollmentsByCourseId(courseId);
        return ResponseEntity.ok(enrollments);
    }

    @Operation(summary = "Assign or update grade",
            description = "Sets final grade (0.00 to 10.00). Grades >= 5.00 update status to COMPLETED; otherwise FAILED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grade updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EnrollmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Grade out of allowed range (0.00 - 10.00)", content = @Content),
            @ApiResponse(responseCode = "409", description = "Cannot update grade for CANCELLED enrollments", content = @Content),
            @ApiResponse(responseCode = "404", description = "Enrollment not found", content = @Content)
    })
    @PatchMapping("/{id}/grade")
    public ResponseEntity<EnrollmentResponseDTO> updateGrade(
            @Parameter(description = "ID of the enrollment", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateGradeDTO updateGradeDTO) {
        EnrollmentResponseDTO updatedEnrollment = enrollmentService.updateGrade(id, updateGradeDTO);
        return ResponseEntity.ok(updatedEnrollment);
    }

    @Operation(summary = "Cancel student enrollment",
            description = "Cancels selected enrollment and automatically releases occupied seat capacity in the course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enrollment cancelled and course seat released",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EnrollmentResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Enrollment is already CANCELLED", content = @Content),
            @ApiResponse(responseCode = "404", description = "Enrollment not found", content = @Content)
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EnrollmentResponseDTO> cancelEnrollment(
            @Parameter(description = "ID of the enrollment to cancel", example = "1") @PathVariable Long id) {
        EnrollmentResponseDTO cancelledEnrollment = enrollmentService.cancelEnrollment(id);
        return ResponseEntity.ok(cancelledEnrollment);
    }
}