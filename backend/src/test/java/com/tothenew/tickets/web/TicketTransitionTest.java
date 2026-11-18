package com.tothenew.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketStatus;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** FR-8, FR-9, acceptance criteria 9, 10 and 14. Expected results come from spec/state-machine.md. */
class TicketTransitionTest extends AbstractApiTest {

    private static final Set<String> ALLOWED = Set.of(
            "OPEN->IN_PROGRESS", "OPEN->CANCELLED",
            "IN_PROGRESS->RESOLVED", "IN_PROGRESS->CANCELLED",
            "RESOLVED->CLOSED");

    static Stream<Arguments> allowed() {
        return pairs(true);
    }

    static Stream<Arguments> rejected() {
        return pairs(false);
    }

    private static Stream<Arguments> pairs(boolean wanted) {
        return Stream.of(TicketStatus.values()).flatMap(from -> Stream.of(TicketStatus.values())
                .filter(to -> ALLOWED.contains(from + "->" + to) == wanted)
                .map(to -> Arguments.of(from, to)));
    }

    private ResultActions transition(long id, String body) throws Exception {
        return mvc.perform(post("/api/tickets/{id}/transitions", id)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions transition(long id, TicketStatus target) throws Exception {
        return transition(id, "{\"targetStatus\":\"" + target + "\"}");
    }

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @MethodSource("allowed")
    void allowedTransitionSucceedsAndPersists(TicketStatus from, TicketStatus to) throws Exception {
        Ticket t = seed(from);
        transition(t.getId(), to)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(to.name()));
        assertThat(tickets.findById(t.getId()).orElseThrow().getStatus()).isEqualTo(to);
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @MethodSource("rejected")
    void disallowedTransitionIsRejectedAndStatusUnchanged(TicketStatus from, TicketStatus to) throws Exception {
        Ticket t = seed(from);
        transition(t.getId(), to)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(from + " to " + to)));
        assertThat(tickets.findById(t.getId()).orElseThrow().getStatus()).isEqualTo(from);
    }

    @Test
    void rejectionMessageListsAllowedTargets() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        transition(t.getId(), TicketStatus.CLOSED)
                .andExpect(jsonPath("$.message")
                        .value("Cannot transition from OPEN to CLOSED. Allowed: IN_PROGRESS, CANCELLED"));
    }

    @Test
    void responseExposesAllowedTransitionsInLifecycleOrder() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        transition(t.getId(), TicketStatus.IN_PROGRESS)
                .andExpect(jsonPath("$.allowedTransitions[0]").value("RESOLVED"))
                .andExpect(jsonPath("$.allowedTransitions[1]").value("CANCELLED"))
                .andExpect(jsonPath("$.allowedTransitions.length()").value(2));
        Ticket closed = seed(TicketStatus.CLOSED);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tickets/{id}", closed.getId()))
                .andExpect(jsonPath("$.allowedTransitions").isEmpty());
    }

    @Test
    void fullHappyPathOpenToClosedWorksStepByStep() throws Exception {
        long id = createViaApi(json("Lifecycle", "walk the path"));
        transition(id, TicketStatus.IN_PROGRESS).andExpect(status().isOk());
        transition(id, TicketStatus.RESOLVED).andExpect(status().isOk());
        transition(id, TicketStatus.CLOSED).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        transition(id, TicketStatus.OPEN).andExpect(status().isConflict());
    }

    @Test
    void transitionBumpsUpdatedAt() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        java.time.Instant past = backdate(t.getId());
        transition(t.getId(), TicketStatus.IN_PROGRESS).andExpect(status().isOk());
        Ticket saved = tickets.findById(t.getId()).orElseThrow();
        assertThat(saved.getUpdatedAt()).isAfter(past);
        assertThat(saved.getVersion()).isEqualTo(t.getVersion() + 1);
    }

    @Test
    void unknownTargetStatusReturns400() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        transition(t.getId(), "{\"targetStatus\":\"DONE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertThat(tickets.findById(t.getId()).orElseThrow().getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void missingTargetStatusReturns400WithFieldError() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        transition(t.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("targetStatus"));
    }

    @Test
    void transitionOnUnknownTicketReturns404() throws Exception {
        transition(777777, TicketStatus.IN_PROGRESS)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }
}
