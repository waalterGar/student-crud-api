package com.students.crud_students.controller;

import com.students.crud_students.dto.NotificationResponseDTO;
import com.students.crud_students.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/students/{studentId}/notifications")
    public ResponseEntity<Page<NotificationResponseDTO>> getNotificationsByStudent(
            @PathVariable Long studentId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getNotificationsByStudent(studentId, pageable));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}