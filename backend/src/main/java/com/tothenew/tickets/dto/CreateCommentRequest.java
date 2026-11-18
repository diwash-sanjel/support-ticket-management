package com.tothenew.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank @Size(max = 100) String author,
        @NotBlank @Size(max = 2000) String body) {
}
