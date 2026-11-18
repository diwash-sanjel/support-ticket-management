package com.tothenew.tickets.service;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketComment;
import com.tothenew.tickets.dto.CommentResponse;
import com.tothenew.tickets.dto.CreateCommentRequest;
import com.tothenew.tickets.exception.TicketNotFoundException;
import com.tothenew.tickets.repository.TicketCommentRepository;
import com.tothenew.tickets.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final TicketRepository tickets;
    private final TicketCommentRepository comments;

    public CommentService(TicketRepository tickets, TicketCommentRepository comments) {
        this.tickets = tickets;
        this.comments = comments;
    }

    /** Comments are allowed in every status, including CLOSED and CANCELLED (spec A3). */
    @Transactional
    public CommentResponse add(Long ticketId, CreateCommentRequest req) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
        return CommentResponse.from(comments.save(new TicketComment(ticket, req.author().strip(), req.body().strip())));
    }
}
