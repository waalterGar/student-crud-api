package com.students.crud_students.controller;

import com.students.crud_students.dto.CreateCourseDTO;
import com.students.crud_students.dto.CourseResponseDTO;
import com.students.crud_students.model.CourseStatus;
import com.students.crud_students.service.CourseService;
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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Endpoints for academic course management")
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "Create a new course", description = "Registers a new course in the academic catalog specifying max capacity and credits.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Course created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input payload or course code already exists", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CourseResponseDTO> createCourse(@Valid @RequestBody CreateCourseDTO createCourseDTO) {
        CourseResponseDTO createdCourse = courseService.createCourse(createCourseDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @Operation(summary = "Get course by ID", description = "Retrieves course details by its unique numeric identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponseDTO> getCourseById(
            @Parameter(description = "ID of the course", example = "1") @PathVariable Long id) {
        CourseResponseDTO course = courseService.getCourseById(id);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Get course by code", description = "Finds a course by its unique functional code (e.g., CS101).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content)
    })
    @GetMapping("/code/{code}")
    public ResponseEntity<CourseResponseDTO> getCourseByCode(
            @Parameter(description = "Unique code of the course", example = "CS101") @PathVariable String code) {
        CourseResponseDTO course = courseService.getCourseByCode(code);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "List courses", description = "Returns the general list of courses. Optionally supports filtering by status (ACTIVE, CLOSED, CANCELLED).")
    @ApiResponse(responseCode = "200", description = "Course list retrieved successfully")
    @GetMapping
    public ResponseEntity<List<CourseResponseDTO>> getAllCourses(
            @Parameter(description = "Course status for optional filtering", example = "ACTIVE")
            @RequestParam(required = false) CourseStatus status) {
        if (status != null) {
            return ResponseEntity.ok(courseService.getCoursesByStatus(status));
        }
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @Operation(summary = "Manually close a course",
              description = "Marks course as CLOSED with MANUAL reason. Frees will not reopen the course automatically.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course closed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Course is CANCELLED (terminal state)", content = @Content)
    })
    @PatchMapping("/{id}/close")
    public ResponseEntity<CourseResponseDTO> closeCourse(
            @Parameter(description = "ID of the course to close", example = "1") @PathVariable Long id) {
        CourseResponseDTO course = courseService.updateCourseStatus(id, CourseStatus.CLOSED);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Reactivate a closed course",
            description = "Transitions a CLOSED course back to ACTIVE if available seats exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course reactivated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Course is CANCELLED or at maximum capacity", content = @Content)
    })
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<CourseResponseDTO> reactivateCourse(
            @Parameter(description = "ID of the course to reactivate", example = "1") @PathVariable Long id) {
        CourseResponseDTO course = courseService.updateCourseStatus(id, CourseStatus.ACTIVE);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Cancel a course",
            description = "Permanently cancels a course, resets capacity to 0, and triggers cascade cancellation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course cancelled successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content)
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<CourseResponseDTO> cancelCourse(
            @Parameter(description = "ID of the course to cancel", example = "1") @PathVariable Long id) {
        CourseResponseDTO course = courseService.updateCourseStatus(id, CourseStatus.CANCELLED);
        return ResponseEntity.ok(course);
    }
}