package com.tothenew.tickets.exception;

import com.tothenew.tickets.domain.TicketStatus;
import java.util.stream.Collectors;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super("Cannot transition from %s to %s. Allowed: %s".formatted(from, to, allowed(from)));
    }

    private static String allowed(TicketStatus from) {
        if (from.allowedTransitions().isEmpty()) {
            return "none";
        }
        return from.allowedTransitions().stream().map(Enum::name).collect(Collectors.joining(", "));
    }
}
