package com.students.crud_students.controller;

import com.students.crud_students.dto.NotificationBatchResponseDTO;
import com.students.crud_students.dto.NotificationResponseDTO;
import com.students.crud_students.dto.PageResponse;
import com.students.crud_students.exception.ApiError;
import com.students.crud_students.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for querying and managing student notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get paginated notifications by student",
            description = "Retrieves a paginated list of notifications for a specific student."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter type",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/students/{studentId}/notifications")
    public ResponseEntity<PageResponse<NotificationResponseDTO>> getNotificationsByStudent(
            @Parameter(description = "ID of the student", example = "1")
            @PathVariable Long studentId,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getNotificationsByStudent(studentId, pageable));
    }

    @Operation(
            summary = "Mark a notification as read",
            description = "Marks a single notification as read by its notification ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "400", description = "Invalid notification ID format",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @Parameter(description = "ID of the notification", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @Operation(
            summary = "Mark all notifications as read for a student",
            description = "Bulk updates all unread notifications to read status for the given student ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All unread notifications marked as read"),
            @ApiResponse(responseCode = "400", description = "Invalid student ID format",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Student not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/students/{studentId}/notifications/read-all")
    public ResponseEntity<NotificationBatchResponseDTO> markAllAsRead(
            @Parameter(description = "ID of the student", example = "1")
            @PathVariable Long studentId
    ) {
        return ResponseEntity.ok(notificationService.markAllAsRead(studentId));
    }
}