package com.tothenew.tickets.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketPriority;
import com.tothenew.tickets.domain.TicketStatus;
import com.tothenew.tickets.repository.TicketCommentRepository;
import com.tothenew.tickets.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractApiTest {

    @Autowired protected MockMvc mvc;
    @Autowired protected TicketRepository tickets;
    @Autowired protected TicketCommentRepository comments;
    @Autowired protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        comments.deleteAll();
        tickets.deleteAll();
    }

    protected Ticket seed(String title, String description, TicketStatus status) {
        Ticket t = tickets.saveAndFlush(new Ticket(title, description, TicketPriority.MEDIUM, null));
        // Seed any status directly; production code can only reach it through the state machine.
        jdbc.update("update ticket set status = ? where id = ?", status.name(), t.getId());
        return tickets.findById(t.getId()).orElseThrow();
    }

    /** Pushes updated_at far into the past so a later strict "is newer" assertion is meaningful. */
    protected java.time.Instant backdate(long id) {
        java.time.Instant past = java.time.Instant.parse("2020-01-01T00:00:00Z");
        jdbc.update("update ticket set updated_at = ? where id = ?", java.sql.Timestamp.from(past), id);
        return past;
    }

    protected Ticket seed(TicketStatus status) {
        return seed("Seeded ticket", "Seeded description", status);
    }

    protected long createViaApi(String json) throws Exception {
        String body = mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected static String json(String title, String description) {
        return "{\"title\":\"%s\",\"description\":\"%s\"}".formatted(title, description);
    }
}
