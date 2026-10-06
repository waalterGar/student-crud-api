package com.students.crud_students.model;

public enum NotificationType {
    // Lifecycle / Onboarding
    WELCOME,                // Bienvenida tras el registro inicial

    // Academic & Enrollment
    COURSE_ENROLLED,        // Confirmación de matriculación en asignatura/curso
    GRADE_PUBLISHED,        // Calificación o nota publicada

    // Administrative & Billing
    PAYMENT_DUE,            // Aviso de pago o cuota pendiente
    DOCUMENT_REQUESTED,     // Requerimiento de documentación (expediente, título, etc.)

    // Operational / Alerts
    ACADEMIC_ALERT,         // Riesgo académico, faltas de asistencia o incidencias
    SYSTEM_ANNOUNCEMENT     // Comunicados generales o mantenimiento de la plataforma
}