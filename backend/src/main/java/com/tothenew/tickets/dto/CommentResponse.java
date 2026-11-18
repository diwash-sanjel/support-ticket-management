package com.tothenew.tickets.dto;

import com.tothenew.tickets.domain.TicketComment;
import java.time.Instant;

public record CommentResponse(Long id, String author, String body, Instant createdAt) {

    public static CommentResponse from(TicketComment c) {
        return new CommentResponse(c.getId(), c.getAuthor(), c.getBody(), c.getCreatedAt());
    }
}
