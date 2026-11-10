package com.familyhome.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMemberRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,30}", message = "Username must contain 3–30 letters, digits or underscores") String username,

        @NotBlank @Size(min = 8, max = 72) String password, Boolean driver) {
    public CreateMemberRequest(String username, String password) { this(username, password, false); }
}