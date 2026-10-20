package com.students.crud_students.dto;

import com.students.crud_students.model.StudentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private StudentStatus status;
}