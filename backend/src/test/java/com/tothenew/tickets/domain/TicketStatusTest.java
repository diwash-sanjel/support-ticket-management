package com.tothenew.tickets.domain;

import static com.tothenew.tickets.domain.TicketStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Expected values come from the matrix in spec/state-machine.md, not from the implementation. */
class TicketStatusTest {

    private static final Set<String> ALLOWED = Set.of(
            "OPEN->IN_PROGRESS", "OPEN->CANCELLED",
            "IN_PROGRESS->RESOLVED", "IN_PROGRESS->CANCELLED",
            "RESOLVED->CLOSED");

    static Stream<Arguments> allPairs() {
        return Stream.of(values()).flatMap(from ->
                Stream.of(values()).map(to ->
                        Arguments.of(from, to, ALLOWED.contains(from + "->" + to))));
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @MethodSource("allPairs")
    void transitionMatchesSpecMatrix(TicketStatus from, TicketStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @Test
    void specMatrixHasFiveAllowedAndTwentyRejectedPairs() {
        long allowed = allPairs().filter(a -> (Boolean) a.get()[2]).count();
        assertThat(allPairs().count()).isEqualTo(25);
        assertThat(allowed).isEqualTo(5);
    }

    @Test
    void closedAndCancelledAreTerminal() {
        assertThat(CLOSED.isTerminal()).isTrue();
        assertThat(CANCELLED.isTerminal()).isTrue();
        assertThat(OPEN.isTerminal()).isFalse();
        assertThat(RESOLVED.isTerminal()).isFalse();
    }
}
