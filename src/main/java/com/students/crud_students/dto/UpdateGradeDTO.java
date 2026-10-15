package com.students.crud_students.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateGradeDTO(

    @NotNull(message = "Grade is required")
    @DecimalMin(value = "0.00", message = "Grade must be at least 0.00")
    @DecimalMax(value = "10.00", message = "Grade cannot exceed 10.00")
    BigDecimal grade
) {}