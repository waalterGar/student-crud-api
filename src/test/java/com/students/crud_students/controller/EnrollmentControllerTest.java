package com.students.crud_students.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.students.crud_students.dto.EnrollmentRequestDTO;
import com.students.crud_students.dto.EnrollmentResponseDTO;
import com.students.crud_students.dto.UpdateGradeDTO;
import com.students.crud_students.exception.BusinessRuleViolationException;
import com.students.crud_students.exception.DuplicateResourceException;
import com.students.crud_students.exception.InvalidStateTransitionException;
import com.students.crud_students.exception.ResourceNotFoundException;
import com.students.crud_students.model.EnrollmentStatus;
import com.students.crud_students.service.EnrollmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
@TestPropertySource(properties = "springdoc.api-docs.enabled=false")
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnrollmentService enrollmentService;

    @Nested
    @DisplayName("POST /api/v1/enrollments - Enroll Student")
    class EnrollStudentTests {

        @Test
        @DisplayName("Should return 201 when enrollment request is valid")
        void shouldReturn201WhenValid() throws Exception {
            EnrollmentRequestDTO request = new EnrollmentRequestDTO(1L, 10L);
            EnrollmentResponseDTO response = new EnrollmentResponseDTO(
                    100L, 1L, "Carlos Mendoza", 10L, "CS101", "Introduction to CS",
                    EnrollmentStatus.ACTIVE, null, LocalDateTime.now()
            );

            when(enrollmentService.enrollStudent(any(EnrollmentRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(100)))
                    .andExpect(jsonPath("$.studentId", is(1)))
                    .andExpect(jsonPath("$.courseCode", is("CS101")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should return 400 when studentId or courseId is negative or null")
        void shouldReturn400WhenIdentifiersInvalid() throws Exception {
            EnrollmentRequestDTO negativeStudent = new EnrollmentRequestDTO(-1L, 10L);

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(negativeStudent)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.errors.studentId", notNullValue()));

            EnrollmentRequestDTO nullCourse = new EnrollmentRequestDTO(1L, null);

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(nullCourse)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.courseId", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 when request payload is an empty JSON object")
        void shouldReturn400WhenPayloadIsEmptyObject() throws Exception {
            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.errors.studentId", notNullValue()))
                    .andExpect(jsonPath("$.errors.courseId", notNullValue()));
        }

        @Test
        @DisplayName("Should return 404 when student or course does not exist")
        void shouldReturn404WhenEntityNotFound() throws Exception {
            EnrollmentRequestDTO request = new EnrollmentRequestDTO(99L, 10L);
            when(enrollmentService.enrollStudent(any())).thenThrow(new ResourceNotFoundException("Student not found with id: 99"));

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Student not found with id: 99")));
        }

        @Test
        @DisplayName("Should return 409 when course is full (BusinessRuleViolationException)")
        void shouldReturn409WhenCourseFull() throws Exception {
            EnrollmentRequestDTO request = new EnrollmentRequestDTO(1L, 10L);
            when(enrollmentService.enrollStudent(any())).thenThrow(new BusinessRuleViolationException("Course is full. Capacity: 30"));

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Business Rule Violation")))
                    .andExpect(jsonPath("$.message", is("Course is full. Capacity: 30")));
        }

        @Test
        @DisplayName("Should return 409 when already enrolled (DuplicateResourceException)")
        void shouldReturn409WhenAlreadyEnrolled() throws Exception {
            EnrollmentRequestDTO request = new EnrollmentRequestDTO(1L, 10L);
            when(enrollmentService.enrollStudent(any())).thenThrow(new DuplicateResourceException("Student is already actively enrolled in this course"));

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Resource Conflict")));
        }

        @Test
        @DisplayName("Should return 409 when student is inactive (InvalidStateTransitionException)")
        void shouldReturn409WhenStudentInactive() throws Exception {
            EnrollmentRequestDTO request = new EnrollmentRequestDTO(1L, 10L);
            when(enrollmentService.enrollStudent(any())).thenThrow(new InvalidStateTransitionException("Student is not active. Current status: SUSPENDED"));

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Invalid State Transition")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/{id} - Get Enrollment By ID")
    class GetEnrollmentByIdTests {

        @Test
        @DisplayName("Should return 200 when enrollment exists")
        void shouldReturn200WhenEnrollmentExists() throws Exception {
            EnrollmentResponseDTO response = new EnrollmentResponseDTO(
                    1L, 1L, "Carlos Mendoza", 10L, "CS101", "Introduction to CS",
                    EnrollmentStatus.ACTIVE, null, LocalDateTime.now()
            );

            when(enrollmentService.getEnrollmentById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/enrollments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.courseCode", is("CS101")));
        }

        @Test
        @DisplayName("Should return 404 when enrollment does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            when(enrollmentService.getEnrollmentById(999L))
                    .thenThrow(new ResourceNotFoundException("Enrollment not found with id: 999"));

            mockMvc.perform(get("/api/v1/enrollments/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")))
                    .andExpect(jsonPath("$.message", is("Enrollment not found with id: 999")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/enrollments/{id}/grade - Grade Enrollment")
    class UpdateGradeTests {

        @Test
        @DisplayName("Should return 400 when grade has more than 2 decimal places")
        void shouldReturn400WhenGradeHasMoreThanTwoDecimals() throws Exception {
            String rawJson = "{\"grade\": 4.995}";

            mockMvc.perform(patch("/api/v1/enrollments/1/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rawJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.errors.grade", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 when grade is out of range (< 0.00 or > 10.00)")
        void shouldReturn400WhenGradeOutOfRange() throws Exception {
            String negativeJson = "{\"grade\": -1.00}";
            mockMvc.perform(patch("/api/v1/enrollments/1/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(negativeJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.grade", notNullValue()));

            String exceedJson = "{\"grade\": 10.50}";
            mockMvc.perform(patch("/api/v1/enrollments/1/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(exceedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.grade", notNullValue()));
        }

        @Test
        @DisplayName("Should return 200 when grade is valid (2 decimal places)")
        void shouldReturn200WhenGradeValid() throws Exception {
            UpdateGradeDTO request = new UpdateGradeDTO(new BigDecimal("8.50"));
            EnrollmentResponseDTO response = new EnrollmentResponseDTO(
                    1L, 1L, "Carlos Mendoza", 10L, "CS101", "Introduction to CS",
                    EnrollmentStatus.COMPLETED, new BigDecimal("8.50"), LocalDateTime.now()
            );

            when(enrollmentService.updateGrade(eq(1L), any(UpdateGradeDTO.class))).thenReturn(response);

            mockMvc.perform(patch("/api/v1/enrollments/1/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")))
                    .andExpect(jsonPath("$.grade", is(8.50)));
        }

        @Test
        @DisplayName("Should return 200 when grade is at exact boundary limits (0.00 and 10.00)")
        void shouldReturn200WhenGradeIsAtExactBoundaries() throws Exception {
            EnrollmentResponseDTO minResponse = new EnrollmentResponseDTO(
                    1L, 1L, "Carlos Mendoza", 10L, "CS101", "Introduction to CS",
                    EnrollmentStatus.FAILED, new BigDecimal("0.00"), LocalDateTime.now()
            );
            when(enrollmentService.updateGrade(eq(1L), any(UpdateGradeDTO.class))).thenReturn(minResponse);

            mockMvc.perform(patch("/api/v1/enrollments/1/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grade\": 0.00}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.grade", is(0.00)));

            EnrollmentResponseDTO maxResponse = new EnrollmentResponseDTO(
                    1L, 1L, "Carlos Mendoza", 10L, "CS101", "Introduction to CS",
                    EnrollmentStatus.COMPLETED, new BigDecimal("10.00"), LocalDateTime.now()
            );
            when(enrollmentService.updateGrade(eq(1L), any(UpdateGradeDTO.class))).thenReturn(maxResponse);

            mockMvc.perform(patch("/api/v1/enrollments/1/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grade\": 10.00}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.grade", is(10.00)));
        }

        @Test
        @DisplayName("Should return 409 when enrollment is not ACTIVE (InvalidStateTransitionException)")
        void shouldReturn409WhenNotActive() throws Exception {
            UpdateGradeDTO request = new UpdateGradeDTO(new BigDecimal("7.00"));
            when(enrollmentService.updateGrade(eq(1L), any()))
                    .thenThrow(new InvalidStateTransitionException("Cannot assign grade to an enrollment that is not ACTIVE. Current status: COMPLETED"));

            mockMvc.perform(patch("/api/v1/enrollments/1/grade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Invalid State Transition")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/enrollments/{id}/cancel - Cancel Enrollment")
    class CancelEnrollmentTests {

        @Test
        @DisplayName("Should return 200 when cancelling active enrollment")
        void shouldReturn200WhenCancelSuccessful() throws Exception {
            EnrollmentResponseDTO response = new EnrollmentResponseDTO(
                    1L, 1L, "Carlos Mendoza", 10L, "CS101", "Introduction to CS",
                    EnrollmentStatus.CANCELLED, null, LocalDateTime.now()
            );

            when(enrollmentService.cancelEnrollment(1L)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/enrollments/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));
        }

        @Test
        @DisplayName("Should return 409 when cancelling already cancelled or completed enrollment")
        void shouldReturn409WhenAlreadyTerminal() throws Exception {
            when(enrollmentService.cancelEnrollment(1L))
                    .thenThrow(new InvalidStateTransitionException("Cannot cancel an enrollment that is not ACTIVE. Current status: CANCELLED"));

            mockMvc.perform(patch("/api/v1/enrollments/1/cancel"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Invalid State Transition")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/enrollments/student/{studentId} & /course/{courseId}")
    class QueryEnrollmentsTests {

        @Test
        @DisplayName("Should return 200 with enrollments list for student")
        void shouldReturn200ForStudentEnrollments() throws Exception {
            List<EnrollmentResponseDTO> list = List.of(
                    new EnrollmentResponseDTO(1L, 1L, "Carlos", 10L, "CS101", "CS", EnrollmentStatus.ACTIVE, null, LocalDateTime.now())
            );

            when(enrollmentService.getEnrollmentsByStudentId(1L)).thenReturn(list);

            mockMvc.perform(get("/api/v1/enrollments/student/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].courseCode", is("CS101")));
        }

        @Test
        @DisplayName("Should return 404 when querying enrollments for non-existent student")
        void shouldReturn404WhenStudentNotFound() throws Exception {
            when(enrollmentService.getEnrollmentsByStudentId(999L))
                    .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

            mockMvc.perform(get("/api/v1/enrollments/student/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Student not found with id: 999")));
        }

        @Test
        @DisplayName("Should return 200 with enrollments list for course")
        void shouldReturn200ForCourseEnrollments() throws Exception {
            List<EnrollmentResponseDTO> list = List.of(
                    new EnrollmentResponseDTO(1L, 1L, "Carlos", 10L, "CS101", "CS", EnrollmentStatus.ACTIVE, null, LocalDateTime.now())
            );

            when(enrollmentService.getEnrollmentsByCourseId(10L)).thenReturn(list);

            mockMvc.perform(get("/api/v1/enrollments/course/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].studentName", is("Carlos")));
        }

        @Test
        @DisplayName("Should return 404 when querying enrollments for non-existent course")
        void shouldReturn404WhenCourseNotFound() throws Exception {
            when(enrollmentService.getEnrollmentsByCourseId(999L))
                    .thenThrow(new ResourceNotFoundException("Course not found with id: 999"));

            mockMvc.perform(get("/api/v1/enrollments/course/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Course not found with id: 999")));
        }
    }

    @Nested
    @DisplayName("Framework Web Exceptions Handling")
    class FrameworkExceptionTests {

        @Test
        @DisplayName("Should return 405 Method Not Allowed when calling POST on GET endpoint")
        void shouldReturn405WhenMethodNotSupported() throws Exception {
            mockMvc.perform(post("/api/v1/enrollments/1"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status", is(405)))
                    .andExpect(jsonPath("$.error", is("Method Not Allowed")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when path variable is of wrong type")
        void shouldReturn400WhenTypeMismatch() throws Exception {
            mockMvc.perform(get("/api/v1/enrollments/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Invalid Parameter Type")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when request body is malformed JSON")
        void shouldReturn400WhenMalformedJson() throws Exception {
            String malformedJson = "{ studentId: ";

            mockMvc.perform(post("/api/v1/enrollments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Malformed Request")));
        }
    }
}