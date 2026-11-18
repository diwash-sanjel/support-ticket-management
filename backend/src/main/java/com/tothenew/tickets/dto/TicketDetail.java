package com.tothenew.tickets.dto;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketPriority;
import com.tothenew.tickets.domain.TicketStatus;
import java.time.Instant;
import java.util.List;

public record TicketDetail(
        Long id, String title, String description, TicketStatus status, TicketPriority priority,
        String assignee, long version, Instant createdAt, Instant updatedAt,
        List<TicketStatus> allowedTransitions, List<CommentResponse> comments) {

    public static TicketDetail from(Ticket t) {
        return new TicketDetail(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(),
                t.getAssignee(), t.getVersion(), t.getCreatedAt(), t.getUpdatedAt(),
                List.copyOf(t.getStatus().allowedTransitions()),
                t.getComments().stream().map(CommentResponse::from).toList());
    }
}
