package com.tothenew.tickets.web;

import com.tothenew.tickets.domain.TicketStatus;
import com.tothenew.tickets.dto.*;
import com.tothenew.tickets.service.CommentService;
import com.tothenew.tickets.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService tickets;
    private final CommentService comments;

    public TicketController(TicketService tickets, CommentService comments) {
        this.tickets = tickets;
        this.comments = comments;
    }

    @PostMapping
    public ResponseEntity<TicketDetail> create(@Valid @RequestBody CreateTicketRequest req) {
        TicketDetail created = tickets.create(req);
        return ResponseEntity.created(URI.create("/api/tickets/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<TicketSummary> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return tickets.list(status, q, page, size);
    }

    @GetMapping("/{id}")
    public TicketDetail get(@PathVariable Long id) {
        return tickets.get(id);
    }

    @PatchMapping("/{id}")
    public TicketDetail update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest req) {
        return tickets.update(id, req);
    }

    @PostMapping("/{id}/transitions")
    public TicketDetail transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest req) {
        return tickets.transition(id, req.targetStatus());
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long id,
                                                      @Valid @RequestBody CreateCommentRequest req) {
        return ResponseEntity.status(201).body(comments.add(id, req));
    }
}
