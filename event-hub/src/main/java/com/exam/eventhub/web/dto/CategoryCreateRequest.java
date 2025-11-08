package com.exam.eventhub.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
    private String name;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a valid hex code")
    private String color;
}
