package com.tothenew.tickets.dto;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketPriority;
import com.tothenew.tickets.domain.TicketStatus;
import java.time.Instant;

public record TicketSummary(
        Long id, String title, TicketStatus status, TicketPriority priority,
        String assignee, Instant createdAt, Instant updatedAt) {

    public static TicketSummary from(Ticket t) {
        return new TicketSummary(t.getId(), t.getTitle(), t.getStatus(), t.getPriority(),
                t.getAssignee(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
