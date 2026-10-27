package com.students.crud_students.controller;

import com.students.crud_students.dto.AcademicHistoryDTO;
import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;
import com.students.crud_students.dto.StudentUpdateDTO;
import com.students.crud_students.exception.ApiError;
import com.students.crud_students.model.StudentStatus;
import com.students.crud_students.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Endpoints for student CRUD operations")
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "Get all students", description = "Retrieves a complete list of registered students.")
    @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @Operation(summary = "Get a student by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student found"),
            @ApiResponse(responseCode = "404", description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @Operation(summary = "Create a new student")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Student created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<StudentResponseDTO> createStudent(@Valid @RequestBody StudentRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(requestDTO));
    }

    @Operation(summary = "Update an existing student")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered by another student",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateDTO requestDTO) {
        return ResponseEntity.ok(studentService.updateStudent(id, requestDTO));
    }

    @Operation(summary = "Soft delete student", description = "Performs a soft delete by setting student status to INACTIVE and triggering cascade enrollment cancellation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Student soft-deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Suspend a student", description = "Changes student status to SUSPENDED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student suspended successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invalid status transition (e.g. student is INACTIVE)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<StudentResponseDTO> suspendStudent(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.changeStatus(id, StudentStatus.SUSPENDED));
    }

    @Operation(summary = "Reactivate a student", description = "Changes student status back to ACTIVE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Student reactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Invalid status transition",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<StudentResponseDTO> reactivateStudent(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.changeStatus(id, StudentStatus.ACTIVE));
    }

    @Operation(summary = "Get student academic history", description = "Retrieves course enrollments and academic summary for a student")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Academic history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}/academic-history")
    public ResponseEntity<AcademicHistoryDTO> getStudentAcademicHistory(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentAcademicHistory(id));
    }
}