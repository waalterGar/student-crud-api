package com.students.crud_students.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.students.crud_students.dto.AcademicHistoryDTO;
import com.students.crud_students.dto.CourseProgressDTO;
import com.students.crud_students.dto.PageResponse;
import com.students.crud_students.dto.StudentRequestDTO;
import com.students.crud_students.dto.StudentResponseDTO;
import com.students.crud_students.dto.StudentUpdateDTO;
import com.students.crud_students.exception.DuplicateResourceException;
import com.students.crud_students.exception.InvalidStateTransitionException;
import com.students.crud_students.exception.ResourceNotFoundException;
import com.students.crud_students.model.EnrollmentStatus;
import com.students.crud_students.model.StudentStatus;
import com.students.crud_students.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@TestPropertySource(properties = "springdoc.api-docs.enabled=false")
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentService studentService;

    @Nested
    @DisplayName("GET /api/v1/students - Paginated Catalog & Single Queries")
    class QueryStudentsTests {

        @Test
        @DisplayName("Should return 200 with paginated list of students")
        void shouldReturn200WithPaginatedStudents() throws Exception {
            List<StudentResponseDTO> list = List.of(
                    new StudentResponseDTO(1L, "Carlos", "Mendoza", "carlos@example.com", 22, StudentStatus.ACTIVE),
                    new StudentResponseDTO(2L, "Ana", "García", "ana@example.com", 20, StudentStatus.ACTIVE)
            );
            PageResponse<StudentResponseDTO> pageResponse = new PageResponse<>(list, 0, 10, 2L, 1, true);

            when(studentService.getAllStudents(any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/v1/students"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].email", is("carlos@example.com")))
                    .andExpect(jsonPath("$.totalElements", is(2)))
                    .andExpect(jsonPath("$.pageNumber", is(0)));
        }

        @Test
        @DisplayName("Should return 200 when student exists by ID")
        void shouldReturn200WhenStudentExists() throws Exception {
            StudentResponseDTO response = new StudentResponseDTO(
                    1L, "Carlos", "Mendoza", "carlos@example.com", 22, StudentStatus.ACTIVE
            );

            when(studentService.getStudentById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/students/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.firstName", is("Carlos")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should return 404 when student ID does not exist")
        void shouldReturn404WhenStudentNotFound() throws Exception {
            when(studentService.getStudentById(999L))
                    .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

            mockMvc.perform(get("/api/v1/students/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")))
                    .andExpect(jsonPath("$.message", is("Student not found with id: 999")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/students - Create Student")
    class CreateStudentTests {

        @Test
        @DisplayName("Should return 201 when student creation request is valid")
        void shouldReturn201WhenValid() throws Exception {
            StudentRequestDTO request = new StudentRequestDTO("Carlos", "Mendoza", "carlos@example.com", 22);
            StudentResponseDTO response = new StudentResponseDTO(
                    1L, "Carlos", "Mendoza", "carlos@example.com", 22, StudentStatus.ACTIVE
            );

            when(studentService.createStudent(any(StudentRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.email", is("carlos@example.com")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should return 400 when fields violate Bean Validation constraints")
        void shouldReturn400WhenValidationFails() throws Exception {
            StudentRequestDTO invalidRequest = new StudentRequestDTO("A", "", "invalid-email", 14);

            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.errors.firstName", notNullValue()))
                    .andExpect(jsonPath("$.errors.lastName", notNullValue()))
                    .andExpect(jsonPath("$.errors.email", notNullValue()))
                    .andExpect(jsonPath("$.errors.age", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 when request payload is an empty JSON object")
        void shouldReturn400WhenPayloadIsEmptyObject() throws Exception {
            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.errors.firstName", notNullValue()))
                    .andExpect(jsonPath("$.errors.lastName", notNullValue()))
                    .andExpect(jsonPath("$.errors.email", notNullValue()))
                    .andExpect(jsonPath("$.errors.age", notNullValue()));
        }

        @Test
        @DisplayName("Should return 409 when email already exists (DuplicateResourceException)")
        void shouldReturn409WhenEmailDuplicate() throws Exception {
            StudentRequestDTO request = new StudentRequestDTO("Carlos", "Mendoza", "carlos@example.com", 22);
            when(studentService.createStudent(any(StudentRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException("Email already registered: carlos@example.com"));

            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Resource Conflict")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/students/{id} - Update Student")
    class UpdateStudentTests {

        @Test
        @DisplayName("Should return 200 when student update request is valid")
        void shouldReturn200WhenUpdateValid() throws Exception {
            StudentUpdateDTO request = new StudentUpdateDTO("Carlos", "Mendoza", "carlos.new@example.com", 23);
            StudentResponseDTO response = new StudentResponseDTO(
                    1L, "Carlos", "Mendoza", "carlos.new@example.com", 23, StudentStatus.ACTIVE
            );

            when(studentService.updateStudent(eq(1L), any(StudentUpdateDTO.class))).thenReturn(response);

            mockMvc.perform(put("/api/v1/students/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("carlos.new@example.com")))
                    .andExpect(jsonPath("$.age", is(23)));
        }

        @Test
        @DisplayName("Should return 400 when update DTO fails validation")
        void shouldReturn400WhenUpdateValidationFails() throws Exception {
            StudentUpdateDTO invalidUpdate = new StudentUpdateDTO("", "M", "not-an-email", 105);

            mockMvc.perform(put("/api/v1/students/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidUpdate)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.errors.firstName", notNullValue()))
                    .andExpect(jsonPath("$.errors.email", notNullValue()))
                    .andExpect(jsonPath("$.errors.age", notNullValue()));
        }

        @Test
        @DisplayName("Should return 404 when student to update does not exist")
        void shouldReturn404WhenStudentToUpdateNotFound() throws Exception {
            StudentUpdateDTO request = new StudentUpdateDTO("Carlos", "Mendoza", "carlos@example.com", 22);
            when(studentService.updateStudent(eq(999L), any(StudentUpdateDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

            mockMvc.perform(put("/api/v1/students/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }

        @Test
        @DisplayName("Should return 409 when updated email collides with another student")
        void shouldReturn409WhenUpdatedEmailDuplicate() throws Exception {
            StudentUpdateDTO request = new StudentUpdateDTO("Carlos", "Mendoza", "ana@example.com", 22);
            when(studentService.updateStudent(eq(1L), any(StudentUpdateDTO.class)))
                    .thenThrow(new DuplicateResourceException("Email already registered: ana@example.com"));

            mockMvc.perform(put("/api/v1/students/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Resource Conflict")));
        }
    }

    @Nested
    @DisplayName("Lifecycle Operations - Soft Delete, Suspend & Reactivate")
    class LifecycleTests {

        @Test
        @DisplayName("DELETE /api/v1/students/{id} - Should return 204 when soft deleting student")
        void shouldReturn204WhenDeletingStudent() throws Exception {
            doNothing().when(studentService).deleteStudent(1L);

            mockMvc.perform(delete("/api/v1/students/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/v1/students/{id} - Should return 404 when student does not exist")
        void shouldReturn404WhenDeletingNonExistentStudent() throws Exception {
            doThrow(new ResourceNotFoundException("Student not found with id: 999"))
                    .when(studentService).deleteStudent(999L);

            mockMvc.perform(delete("/api/v1/students/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }

        @Test
        @DisplayName("PATCH /api/v1/students/{id}/suspend - Should return 200 when suspending active student")
        void shouldReturn200WhenSuspendingStudent() throws Exception {
            StudentResponseDTO response = new StudentResponseDTO(
                    1L, "Carlos", "Mendoza", "carlos@example.com", 22, StudentStatus.SUSPENDED
            );

            when(studentService.changeStatus(1L, StudentStatus.SUSPENDED)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/students/1/suspend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("SUSPENDED")));
        }

        @Test
        @DisplayName("PATCH /api/v1/students/{id}/suspend - Should return 404 when student does not exist")
        void shouldReturn404WhenSuspendingNonExistentStudent() throws Exception {
            when(studentService.changeStatus(999L, StudentStatus.SUSPENDED))
                    .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

            mockMvc.perform(patch("/api/v1/students/999/suspend"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }

        @Test
        @DisplayName("PATCH /api/v1/students/{id}/suspend - Should return 409 when suspending an INACTIVE student")
        void shouldReturn409WhenSuspendingInactiveStudent() throws Exception {
            when(studentService.changeStatus(1L, StudentStatus.SUSPENDED))
                    .thenThrow(new InvalidStateTransitionException("An INACTIVE student can only be reactivated to ACTIVE status. Current status is INACTIVE."));

            mockMvc.perform(patch("/api/v1/students/1/suspend"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Invalid State Transition")));
        }

        @Test
        @DisplayName("PATCH /api/v1/students/{id}/reactivate - Should return 200 when reactivating student")
        void shouldReturn200WhenReactivatingStudent() throws Exception {
            StudentResponseDTO response = new StudentResponseDTO(
                    1L, "Carlos", "Mendoza", "carlos@example.com", 22, StudentStatus.ACTIVE
            );

            when(studentService.changeStatus(1L, StudentStatus.ACTIVE)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/students/1/reactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("PATCH /api/v1/students/{id}/reactivate - Should return 404 when student does not exist")
        void shouldReturn404WhenReactivatingNonExistentStudent() throws Exception {
            when(studentService.changeStatus(999L, StudentStatus.ACTIVE))
                    .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

            mockMvc.perform(patch("/api/v1/students/999/reactivate"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/students/{id}/academic-history - Academic History")
    class AcademicHistoryTests {

        @Test
        @DisplayName("Should return 200 with student academic history summary")
        void shouldReturn200WithAcademicHistory() throws Exception {
            CourseProgressDTO courseProgress = new CourseProgressDTO(
                    10L, 100L, "CS101", "Introduction to CS",
                    EnrollmentStatus.COMPLETED, new BigDecimal("8.50"), LocalDateTime.now()
            );
            AcademicHistoryDTO history = new AcademicHistoryDTO(
                    1L, "Carlos Mendoza", "carlos@example.com",
                    1, 0, 1, 0, 0,
                    new BigDecimal("8.50"), List.of(courseProgress)
            );

            when(studentService.getStudentAcademicHistory(1L)).thenReturn(history);

            mockMvc.perform(get("/api/v1/students/1/academic-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentId", is(1)))
                    .andExpect(jsonPath("$.studentName", is("Carlos Mendoza")))
                    .andExpect(jsonPath("$.totalEnrolledCourses", is(1)))
                    .andExpect(jsonPath("$.completedCourses", is(1)))
                    .andExpect(jsonPath("$.averageGrade", is(8.50)))
                    .andExpect(jsonPath("$.courses", hasSize(1)))
                    .andExpect(jsonPath("$.courses[0].courseCode", is("CS101")));
        }

        @Test
        @DisplayName("Should return 200 with zeroed metrics when student has no enrollments")
        void shouldReturn200WithEmptyAcademicHistory() throws Exception {
            AcademicHistoryDTO emptyHistory = new AcademicHistoryDTO(
                    1L, "Carlos Mendoza", "carlos@example.com",
                    0, 0, 0, 0, 0,
                    BigDecimal.ZERO, List.of()
            );

            when(studentService.getStudentAcademicHistory(1L)).thenReturn(emptyHistory);

            mockMvc.perform(get("/api/v1/students/1/academic-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEnrolledCourses", is(0)))
                    .andExpect(jsonPath("$.averageGrade", is(0)))
                    .andExpect(jsonPath("$.courses", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404 when student does not exist")
        void shouldReturn404WhenStudentNotFound() throws Exception {
            when(studentService.getStudentAcademicHistory(999L))
                    .thenThrow(new ResourceNotFoundException("Student not found with id: 999"));

            mockMvc.perform(get("/api/v1/students/999/academic-history"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")))
                    .andExpect(jsonPath("$.message", is("Student not found with id: 999")));
        }
    }

    @Nested
    @DisplayName("Framework Web Exceptions Handling")
    class FrameworkExceptionTests {

        @Test
        @DisplayName("Should return 400 Bad Request when request body is malformed JSON")
        void shouldReturn400WhenMalformedJson() throws Exception {
            String malformedJson = "{ firstName: ";

            mockMvc.perform(post("/api/v1/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Malformed Request")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when path variable is of wrong type")
        void shouldReturn400WhenTypeMismatch() throws Exception {
            mockMvc.perform(get("/api/v1/students/invalid-id"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Invalid Parameter Type")));
        }

        @Test
        @DisplayName("Should return 405 Method Not Allowed when executing unsupported HTTP verb")
        void shouldReturn405WhenMethodNotSupported() throws Exception {
            mockMvc.perform(post("/api/v1/students/1"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status", is(405)))
                    .andExpect(jsonPath("$.error", is("Method Not Allowed")));
        }
    }
}