package com.tothenew.tickets.exception;

import com.tothenew.tickets.domain.TicketStatus;

public class TicketNotEditableException extends RuntimeException {
    public TicketNotEditableException(Long id, TicketStatus status) {
        super("Ticket " + id + " is " + status + " and can no longer be edited");
    }
}
