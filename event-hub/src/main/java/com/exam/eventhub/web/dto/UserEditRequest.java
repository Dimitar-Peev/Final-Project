package com.exam.eventhub.web.dto;

import com.exam.eventhub.validation.PhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserEditRequest {

    private UUID id;

    private String username;

    @Email(message = "Please provide a valid email")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(max = 20, message = "First name can'''t have more than 20 symbols")
    private String firstName;

    @Size(max = 20, message = "Last name can'''t have more than 20 symbols")
    private String lastName;

    @PhoneNumber
    private String phoneNumber;

    @URL(message = "Please provide a valid URL")
    private String profileImageUrl;

    private LocalDateTime updatedAt;
}
