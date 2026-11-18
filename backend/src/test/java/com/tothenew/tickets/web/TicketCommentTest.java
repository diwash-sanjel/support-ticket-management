package com.tothenew.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** FR-5. */
class TicketCommentTest extends AbstractApiTest {

    private ResultActions comment(long id, String body) throws Exception {
        return mvc.perform(post("/api/tickets/{id}/comments", id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void addsCommentAndReturns201() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        comment(t.getId(), "{\"author\":\"Ann\",\"body\":\"Looking into it\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.author").value("Ann"))
                .andExpect(jsonPath("$.body").value("Looking into it"))
                .andExpect(jsonPath("$.createdAt").exists());
        assertThat(comments.count()).isEqualTo(1);
    }

    @Test
    void commentsAppearInDetailInChronologicalOrder() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        comment(t.getId(), "{\"author\":\"A\",\"body\":\"first\"}");
        comment(t.getId(), "{\"author\":\"B\",\"body\":\"second\"}");
        mvc.perform(get("/api/tickets/{id}", t.getId()))
                .andExpect(jsonPath("$.comments.length()").value(2))
                .andExpect(jsonPath("$.comments[0].body").value("first"))
                .andExpect(jsonPath("$.comments[1].body").value("second"));
    }

    @Test
    void commentsAreScopedToTheirTicket() throws Exception {
        Ticket a = seed("A", "d", TicketStatus.OPEN);
        Ticket b = seed("B", "d", TicketStatus.OPEN);
        comment(a.getId(), "{\"author\":\"X\",\"body\":\"only on A\"}");
        mvc.perform(get("/api/tickets/{id}", b.getId()))
                .andExpect(jsonPath("$.comments").isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"CLOSED", "CANCELLED"})
    void commentsAllowedOnTerminalTickets(TicketStatus terminal) throws Exception {
        Ticket t = seed(terminal);
        comment(t.getId(), "{\"author\":\"Ann\",\"body\":\"post-mortem\"}").andExpect(status().isCreated());
    }

    @Test
    void rejectsBlankAuthorAndBody() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        comment(t.getId(), "{\"author\":\" \",\"body\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='author')]").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='body')]").isNotEmpty());
        assertThat(comments.count()).isZero();
    }

    @Test
    void rejectsOversizeBodyAndAuthor() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        comment(t.getId(), "{\"author\":\"" + "a".repeat(101) + "\",\"body\":\"" + "b".repeat(2001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void commentOnMissingTicketReturns404AndStoresNothing() throws Exception {
        comment(123456, "{\"author\":\"A\",\"body\":\"b\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
        assertThat(comments.count()).isZero();
    }

    @Test
    void addingCommentDoesNotChangeTicketVersion() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        comment(t.getId(), "{\"author\":\"A\",\"body\":\"b\"}");
        assertThat(tickets.findById(t.getId()).orElseThrow().getVersion()).isEqualTo(t.getVersion());
    }
}
