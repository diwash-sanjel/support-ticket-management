package com.tothenew.tickets.dto;

import com.tothenew.tickets.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 5000) String description,
        TicketPriority priority,
        @Size(max = 100) String assignee) {
}
