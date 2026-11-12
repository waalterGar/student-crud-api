package com.students.crud_students.controller;

import com.students.crud_students.dto.NotificationBatchResponseDTO;
import com.students.crud_students.dto.NotificationResponseDTO;
import com.students.crud_students.dto.PageResponse;
import com.students.crud_students.exception.ResourceNotFoundException;
import com.students.crud_students.model.NotificationType;
import com.students.crud_students.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@TestPropertySource(properties = "springdoc.api-docs.enabled=false")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Nested
    @DisplayName("GET /api/v1/students/{studentId}/notifications - Get Student Notifications")
    class GetNotificationsByStudentTests {

        @Test
        @DisplayName("Should return 200 with paginated notifications for student")
        void shouldReturn200WithPaginatedNotifications() throws Exception {
            List<NotificationResponseDTO> list = List.of(
                    new NotificationResponseDTO(
                            1L, "Welcome to the platform!", NotificationType.WELCOME,
                            false, 1L, LocalDateTime.now()
                    )
            );
            PageResponse<NotificationResponseDTO> pageResponse = new PageResponse<>(list, 0, 10, 1L, 1, true);

            when(notificationService.getNotificationsByStudent(eq(1L), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/v1/students/1/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].type", is("WELCOME")))
                    .andExpect(jsonPath("$.content[0].read", is(false)))
                    .andExpect(jsonPath("$.totalElements", is(1)))
                    .andExpect(jsonPath("$.pageNumber", is(0)));
        }

        @Test
        @DisplayName("Should return 404 when student does not exist")
        void shouldReturn404WhenStudentNotFound() throws Exception {
            when(notificationService.getNotificationsByStudent(eq(999L), any(Pageable.class)))
                    .thenThrow(new ResourceNotFoundException("Student not found with ID: 999"));

            mockMvc.perform(get("/api/v1/students/999/notifications"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")))
                    .andExpect(jsonPath("$.message", is("Student not found with ID: 999")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/{id}/read - Mark Single Notification As Read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should return 200 when notification is marked as read")
        void shouldReturn200WhenMarkedAsRead() throws Exception {
            NotificationResponseDTO response = new NotificationResponseDTO(
                    1L, "Enrolled in CS101", NotificationType.COURSE_ENROLLED,
                    true, 1L, LocalDateTime.now()
            );

            when(notificationService.markAsRead(1L)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/notifications/1/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.read", is(true)));
        }

        @Test
        @DisplayName("Should return 404 when notification does not exist")
        void shouldReturn404WhenNotificationNotFound() throws Exception {
            when(notificationService.markAsRead(999L))
                    .thenThrow(new ResourceNotFoundException("Notification not found with ID: 999"));

            mockMvc.perform(patch("/api/v1/notifications/999/read"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")))
                    .andExpect(jsonPath("$.message", is("Notification not found with ID: 999")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/students/{studentId}/notifications/read-all - Bulk Mark As Read")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should return 200 with batch summary when all student notifications are marked as read")
        void shouldReturn200WhenAllMarkedAsRead() throws Exception {
            NotificationBatchResponseDTO response = new NotificationBatchResponseDTO(
                    1L, 3, "Successfully marked 3 notifications as read"
            );

            when(notificationService.markAllAsRead(1L)).thenReturn(response);

            mockMvc.perform(patch("/api/v1/students/1/notifications/read-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentId", is(1)))
                    .andExpect(jsonPath("$.updatedCount", is(3)))
                    .andExpect(jsonPath("$.message", is("Successfully marked 3 notifications as read")));
        }

        @Test
        @DisplayName("Should return 404 when student does not exist")
        void shouldReturn404WhenStudentNotFound() throws Exception {
            when(notificationService.markAllAsRead(999L))
                    .thenThrow(new ResourceNotFoundException("Student not found with ID: 999"));

            mockMvc.perform(patch("/api/v1/students/999/notifications/read-all"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Resource Not Found")))
                    .andExpect(jsonPath("$.message", is("Student not found with ID: 999")));
        }
    }

    @Nested
    @DisplayName("Framework Web Exceptions Handling")
    class FrameworkExceptionTests {

        @Test
        @DisplayName("Should return 400 Bad Request when path variable is not numeric")
        void shouldReturn400WhenStudentIdNotNumeric() throws Exception {
            mockMvc.perform(get("/api/v1/students/invalid-id/notifications"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Invalid Parameter Type")));
        }

        @Test
        @DisplayName("Should return 405 Method Not Allowed when executing unsupported HTTP verb")
        void shouldReturn405WhenMethodNotSupported() throws Exception {
            mockMvc.perform(post("/api/v1/notifications/1/read"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status", is(405)))
                    .andExpect(jsonPath("$.error", is("Method Not Allowed")));
        }
    }
}