package com.tothenew.tickets.dto;

import com.tothenew.tickets.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest(@NotNull TicketStatus targetStatus) {
}
