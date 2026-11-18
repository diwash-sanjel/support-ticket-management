package com.tothenew.tickets.web;

import com.tothenew.tickets.domain.TicketStatus;
import com.tothenew.tickets.exception.InvalidStatusTransitionException;
import com.tothenew.tickets.exception.TicketNotEditableException;
import com.tothenew.tickets.exception.TicketNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only controller that triggers each exception so the handler's output can be verified. */
@RestController
class ThrowingTestController {

    @GetMapping("/test-errors/not-found")
    void notFound() { throw new TicketNotFoundException(5L); }

    @GetMapping("/test-errors/transition")
    void transition() { throw new InvalidStatusTransitionException(TicketStatus.CLOSED, TicketStatus.OPEN); }

    @GetMapping("/test-errors/not-editable")
    void notEditable() { throw new TicketNotEditableException(5L, TicketStatus.CLOSED); }

    @GetMapping("/test-errors/locking")
    void locking() { throw new OptimisticLockingFailureException("stale"); }

    @GetMapping("/test-errors/boom")
    void boom() { throw new IllegalStateException("secret internal detail"); }
}
