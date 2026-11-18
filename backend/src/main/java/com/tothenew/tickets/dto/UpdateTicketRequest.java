package com.tothenew.tickets.dto;

import com.tothenew.tickets.domain.TicketPriority;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update. A null field means "leave unchanged". An empty assignee string clears the assignee.
 * Status is deliberately absent: unknown properties are rejected, lifecycle goes through /transitions.
 */
public record UpdateTicketRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") @Size(max = 120) String title,
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") @Size(max = 5000) String description,
        TicketPriority priority,
        @Size(max = 100) String assignee) {

    public boolean isEmpty() {
        return title == null && description == null && priority == null && assignee == null;
    }
}
