package com.tothenew.tickets.service;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketStatus;
import com.tothenew.tickets.dto.*;
import com.tothenew.tickets.exception.InvalidRequestException;
import com.tothenew.tickets.exception.TicketNotEditableException;
import com.tothenew.tickets.exception.TicketNotFoundException;
import com.tothenew.tickets.repository.TicketRepository;
import com.tothenew.tickets.repository.TicketSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository tickets;

    public TicketService(TicketRepository tickets) {
        this.tickets = tickets;
    }

    @Transactional
    public TicketDetail create(CreateTicketRequest req) {
        Ticket ticket = new Ticket(req.title().strip(), req.description().strip(), req.priority(),
                blankToNull(req.assignee()));
        return TicketDetail.from(tickets.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketDetail get(Long id) {
        return TicketDetail.from(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummary> list(TicketStatus status, String keyword, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.from(tickets.findAll(TicketSpecifications.filter(status, keyword), pageable)
                .map(TicketSummary::from));
    }

    @Transactional
    public TicketDetail update(Long id, UpdateTicketRequest req) {
        if (req.isEmpty()) {
            throw new InvalidRequestException("At least one of title, description, priority, assignee is required");
        }
        Ticket ticket = find(id);
        if (ticket.getStatus().isTerminal()) {
            throw new TicketNotEditableException(id, ticket.getStatus());
        }
        if (req.title() != null) {
            ticket.setTitle(req.title().strip());
        }
        if (req.description() != null) {
            ticket.setDescription(req.description().strip());
        }
        if (req.priority() != null) {
            ticket.setPriority(req.priority());
        }
        if (req.assignee() != null) {
            ticket.setAssignee(blankToNull(req.assignee()));
        }
        return TicketDetail.from(tickets.saveAndFlush(ticket));
    }

    @Transactional
    public TicketDetail transition(Long id, TicketStatus target) {
        Ticket ticket = find(id);
        ticket.transitionTo(target);
        // Flush inside the transaction so an optimistic-lock conflict surfaces here and maps to 409.
        return TicketDetail.from(tickets.saveAndFlush(ticket));
    }

    Ticket find(Long id) {
        return tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
