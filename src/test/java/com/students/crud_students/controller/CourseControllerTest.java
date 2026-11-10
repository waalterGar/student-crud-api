package com.students.crud_students.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.students.crud_students.dto.CourseResponseDTO;
import com.students.crud_students.dto.CreateCourseDTO;
import com.students.crud_students.exception.BusinessRuleViolationException;
import com.students.crud_students.exception.DuplicateResourceException;
import com.students.crud_students.exception.InvalidStateTransitionException;
import com.students.crud_students.exception.ResourceNotFoundException;
import com.students.crud_students.model.CourseStatus;
import com.students.crud_students.service.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@TestPropertySource(properties = "springdoc.api-docs.enabled=false")
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseService courseService;

    @Nested
    @DisplayName("POST /api/v1/courses - Create Course")
    class CreateCourseTests {

        @Test
        @DisplayName("Should return 201 when course creation request is valid")
        void shouldReturn201WhenValid() throws Exception {
            CreateCourseDTO request = new CreateCourseDTO("CS101", "Introduction to Computer Science", 4, 30);
            CourseResponseDTO response = new CourseResponseDTO(
                    1L, "CS101", "Introduction to Computer Science", 4, 30, 0,
                    CourseStatus.ACTIVE, LocalDateTime.now()
            );

            when(courseService.createCourse(any(CreateCourseDTO.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.code", is("CS101")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")))
                    .andExpect(jsonPath("$.currentEnrolled", is(0)));
        }

        @Test
        @DisplayName("Should return 400 when code or title is blank or credits/capacity non-positive")
        void shouldReturn400WhenInputInvalid() throws Exception {
            CreateCourseDTO invalidRequest = new CreateCourseDTO("", "   ", -2, 0);

            mockMvc.perform(post("/api/v1/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.errors.code", notNullValue()))
                    .andExpect(jsonPath("$.errors.title", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 when request payload is malformed JSON")
        void shouldReturn400WhenMalformedJson() throws Exception {
            String malformedJson = "{ code: ";

            mockMvc.perform(post("/api/v1/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Malformed Request")));
        }

        @Test
        @DisplayName("Should return 409 when course code already exists (DuplicateResourceException)")
        void shouldReturn409WhenDuplicateCode() throws Exception {
            CreateCourseDTO request = new CreateCourseDTO("CS101", "Intro to CS", 4, 30);
            when(courseService.createCourse(any()))
                    .thenThrow(new DuplicateResourceException("Course with code CS101 already exists"));

            mockMvc.perform(post("/api/v1/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Resource Conflict")))
                    .andExpect(jsonPath("$.message", is("Course with code CS101 already exists")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/courses - Queries & Filters")
    class QueryCoursesTests {

        @Test
        @DisplayName("Should return 200 with course details by ID")
        void shouldReturn200ForCourseById() throws Exception {
            CourseResponseDTO response = new CourseResponseDTO(
                    1L, "CS101", "Introduction to CS", 4, 30, 5,
                    CourseStatus.ACTIVE, LocalDateTime.now()
            );

            when(courseService.getCourseById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/courses/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.code", is("CS101")));
        }

        @Test
        @DisplayName("Should return 404 when course ID does not exist")
        void shouldReturn404WhenIdNotFound() throws Exception {
            when(courseService.getCourseById(999L))
                    .thenThrow(new ResourceNotFoundException("Course not found with id: 999"));

            mockMvc.perform(get("/api/v1/courses/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Course not found with id: 999")));
        }

        @Test
        @DisplayName("Should return 200 with course details by code")
        void shouldReturn200ForCourseByCode() throws Exception {
            CourseResponseDTO response = new CourseResponseDTO(
                    1L, "CS101", "Introduction to CS", 4, 30, 5,
                    CourseStatus.ACTIVE, LocalDateTime.now()
            );

            when(courseService.getCourseByCode("CS101")).thenReturn(response);

            mockMvc.perform(get("/api/v1/courses/code/CS101"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("CS101")));
        }

        @Test
        @DisplayName("Should return 404 when course code does not exist")
        void shouldReturn404WhenCodeNotFound() throws Exception {
            when(courseService.getCourseByCode("UNKNOWN"))
                    .thenThrow(new ResourceNotFoundException("Course not found with code: UNKNOWN"));

            mockMvc.perform(get("/api/v1/courses/code/UNKNOWN"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }

        @Test
        @DisplayName("Should return 200 with all courses without filter")
        void shouldReturn200ForAllCourses() throws Exception {
            List<CourseResponseDTO> list = List.of(
                    new CourseResponseDTO(1L, "CS101", "CS1", 4, 30, 0, CourseStatus.ACTIVE, LocalDateTime.now()),
                    new CourseResponseDTO(2L, "MATH101", "Math", 6, 25, 0, CourseStatus.ACTIVE, LocalDateTime.now())
            );

            when(courseService.getAllCourses()).thenReturn(list);

            mockMvc.perform(get("/api/v1/courses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Should return 200 with courses filtered by status")
        void shouldReturn200ForFilteredCourses() throws Exception {
            List<CourseResponseDTO> list = List.of(
                    new CourseResponseDTO(1L, "CS101", "CS1", 4, 30, 0, CourseStatus.ACTIVE, LocalDateTime.now())
            );

            when(courseService.getCoursesByStatus(CourseStatus.ACTIVE)).thenReturn(list);

            mockMvc.perform(get("/api/v1/courses?status=ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when status query param is invalid")
        void shouldReturn400WhenStatusQueryParamInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/courses?status=INVALID_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Invalid Parameter Type")));
        }
    }

    @Nested
    @DisplayName("Lifecycle Transitions - Close, Reactivate, Cancel")
    class LifecycleTests {

        @Test
        @DisplayName("PATCH /api/v1/courses/{id}/close - Should return 200 when manually closing course")
        void shouldReturn200WhenClosingCourse() throws Exception {
            CourseResponseDTO response = new CourseResponseDTO(
                    1L, "CS101", "CS1", 4, 30, 10, CourseStatus.CLOSED, LocalDateTime.now()
            );

            when(courseService.updateCourseStatus(1L, CourseStatus.CLOSED)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/courses/1/close"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CLOSED")));
        }

        @Test
        @DisplayName("PATCH /api/v1/courses/{id}/close - Should return 409 when closing a CANCELLED course")
        void shouldReturn409WhenClosingCancelledCourse() throws Exception {
            when(courseService.updateCourseStatus(1L, CourseStatus.CLOSED))
                    .thenThrow(new InvalidStateTransitionException("A CANCELLED course cannot transition to any other status."));

            mockMvc.perform(patch("/api/v1/courses/1/close"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Invalid State Transition")));
        }

        @Test
        @DisplayName("PATCH /api/v1/courses/{id}/reactivate - Should return 200 when reactivating course")
        void shouldReturn200WhenReactivatingCourse() throws Exception {
            CourseResponseDTO response = new CourseResponseDTO(
                    1L, "CS101", "CS1", 4, 30, 10, CourseStatus.ACTIVE, LocalDateTime.now()
            );

            when(courseService.updateCourseStatus(1L, CourseStatus.ACTIVE)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/courses/1/reactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("PATCH /api/v1/courses/{id}/reactivate - Should return 409 when course is at full capacity")
        void shouldReturn409WhenReactivatingFullCourse() throws Exception {
            when(courseService.updateCourseStatus(1L, CourseStatus.ACTIVE))
                    .thenThrow(new BusinessRuleViolationException("Cannot reactivate course. Course is at maximum capacity (30/30)."));

            mockMvc.perform(patch("/api/v1/courses/1/reactivate"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Business Rule Violation")));
        }

        @Test
        @DisplayName("PATCH /api/v1/courses/{id}/reactivate - Should return 409 when reactivating a CANCELLED course")
        void shouldReturn409WhenReactivatingCancelledCourse() throws Exception {
            when(courseService.updateCourseStatus(1L, CourseStatus.ACTIVE))
                    .thenThrow(new InvalidStateTransitionException("A CANCELLED course cannot transition to any other status."));

            mockMvc.perform(patch("/api/v1/courses/1/reactivate"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Invalid State Transition")));
        }

        @Test
        @DisplayName("PATCH /api/v1/courses/{id}/cancel - Should return 200 when cancelling course")
        void shouldReturn200WhenCancellingCourse() throws Exception {
            CourseResponseDTO response = new CourseResponseDTO(
                    1L, "CS101", "CS1", 4, 30, 0, CourseStatus.CANCELLED, LocalDateTime.now()
            );

            when(courseService.updateCourseStatus(1L, CourseStatus.CANCELLED)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/courses/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")))
                    .andExpect(jsonPath("$.currentEnrolled", is(0)));
        }

        @Test
        @DisplayName("Should return 404 when updating status of non-existent course")
        void shouldReturn404WhenCourseToUpdateNotFound() throws Exception {
            when(courseService.updateCourseStatus(999L, CourseStatus.CLOSED))
                    .thenThrow(new ResourceNotFoundException("Course not found with id: 999"));

            mockMvc.perform(patch("/api/v1/courses/999/close"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")));
        }
    }

    @Nested
    @DisplayName("Framework Exceptions Handling")
    class FrameworkExceptionTests {

        @Test
        @DisplayName("Should return 400 Bad Request when course ID in path is not numeric")
        void shouldReturn400WhenIdMismatch() throws Exception {
            mockMvc.perform(get("/api/v1/courses/invalid-id"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Invalid Parameter Type")));
        }

        @Test
        @DisplayName("Should return 405 Method Not Allowed when executing unsupported HTTP verb")
        void shouldReturn405WhenMethodNotSupported() throws Exception {
            mockMvc.perform(post("/api/v1/courses/1"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status", is(405)))
                    .andExpect(jsonPath("$.error", is("Method Not Allowed")));
        }
    }
}