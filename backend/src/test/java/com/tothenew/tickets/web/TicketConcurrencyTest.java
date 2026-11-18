package com.tothenew.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Optimistic locking (spec state-machine.md, last rule). */
class TicketConcurrencyTest extends AbstractApiTest {

    @Test
    void staleEntityCannotOverwriteNewerVersion() {
        Ticket seeded = seed(TicketStatus.OPEN);
        Ticket first = tickets.findById(seeded.getId()).orElseThrow();
        Ticket second = tickets.findById(seeded.getId()).orElseThrow();

        first.setTitle("winner");
        tickets.saveAndFlush(first);

        second.setTitle("loser");
        assertThatThrownBy(() -> tickets.saveAndFlush(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(tickets.findById(seeded.getId()).orElseThrow().getTitle()).isEqualTo("winner");
    }

    private record Outcome(int status, String code) {
    }

    private List<Outcome> race(long id, TicketStatus... targets) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(targets.length);
        try {
            CyclicBarrier barrier = new CyclicBarrier(targets.length);
            List<Future<Outcome>> futures = new ArrayList<>();
            for (TicketStatus target : targets) {
                futures.add(pool.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    var res = mvc.perform(post("/api/tickets/{id}/transitions", id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"targetStatus\":\"" + target + "\"}"))
                            .andReturn().getResponse();
                    String code = res.getStatus() == 409 ? JsonPath.read(res.getContentAsString(), "$.code") : "";
                    return new Outcome(res.getStatus(), code);
                }));
            }
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> f : futures) {
                outcomes.add(f.get(10, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    /** Same transition twice at once: exactly one may win; the loser gets a clean 409, never a 500. */
    @Test
    void racingIdenticalTransitionsHaveExactlyOneWinner() throws Exception {
        for (int round = 0; round < 25; round++) {
            long id = seed(TicketStatus.OPEN).getId();
            List<Outcome> outcomes = race(id, TicketStatus.IN_PROGRESS, TicketStatus.IN_PROGRESS);

            assertThat(outcomes.stream().filter(o -> o.status() == 200).count())
                    .as("round %d successes: %s", round, outcomes).isEqualTo(1);
            assertThat(outcomes.stream().filter(o -> o.status() == 409))
                    .as("round %d conflicts: %s", round, outcomes)
                    .singleElement()
                    .satisfies(o -> assertThat(o.code()).isIn("INVALID_STATUS_TRANSITION", "CONCURRENT_MODIFICATION"));
            assertThat(tickets.findById(id).orElseThrow().getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        }
    }

    /**
     * OPEN->IN_PROGRESS raced with OPEN->CANCELLED. Both can legitimately succeed if they serialise as
     * IN_PROGRESS then CANCELLED (a legal chain), so the invariant is consistency, not "one winner":
     * no 500s, at least one success, and the final status matches what the responses say happened.
     */
    @Test
    void racingDifferentTransitionsStayConsistent() throws Exception {
        for (int round = 0; round < 25; round++) {
            long id = seed(TicketStatus.OPEN).getId();
            List<Outcome> outcomes = race(id, TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);

            assertThat(outcomes).as("round %d: %s", round, outcomes)
                    .allSatisfy(o -> assertThat(o.status()).isIn(200, 409));
            long successes = outcomes.stream().filter(o -> o.status() == 200).count();
            assertThat(successes).isGreaterThanOrEqualTo(1);
            TicketStatus finalStatus = tickets.findById(id).orElseThrow().getStatus();
            if (successes == 2) {
                assertThat(finalStatus).as("both succeeded => chained IN_PROGRESS -> CANCELLED")
                        .isEqualTo(TicketStatus.CANCELLED);
            } else {
                assertThat(finalStatus).isIn(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
            }
        }
    }
}
