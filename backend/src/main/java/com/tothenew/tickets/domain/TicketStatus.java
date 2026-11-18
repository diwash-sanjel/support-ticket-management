package com.tothenew.tickets.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Ticket lifecycle. This enum is the single source of truth for allowed transitions
 * (see spec/state-machine.md).
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    /** Allowed targets in lifecycle (declaration) order, so API output is deterministic. */
    public Set<TicketStatus> allowedTransitions() {
        return Collections.unmodifiableSet(switch (this) {
            case OPEN -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(RESOLVED, CANCELLED);
            case RESOLVED -> EnumSet.of(CLOSED);
            case CLOSED, CANCELLED -> EnumSet.noneOf(TicketStatus.class);
        });
    }

    public boolean canTransitionTo(TicketStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
