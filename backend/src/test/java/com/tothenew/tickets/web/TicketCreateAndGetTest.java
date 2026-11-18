package com.tothenew.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketPriority;
import com.tothenew.tickets.domain.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

/** FR-1, FR-3, FR-10. */
class TicketCreateAndGetTest extends AbstractApiTest {

    @Test
    void createsTicketAsOpenWithDefaultMediumPriority() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Printer broken", "Jams on page 2")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/tickets/\\d+")))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.title").value("Printer broken"))
                .andExpect(jsonPath("$.assignee").doesNotExist())
                .andExpect(jsonPath("$.comments").isEmpty());

        assertThat(tickets.findAll()).singleElement().satisfies(t -> {
            assertThat(t.getStatus()).isEqualTo(TicketStatus.OPEN);
            assertThat(t.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void createsTicketWithPriorityAndTrimmedFields() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  Hi  \",\"description\":\" d \",\"priority\":\"CRITICAL\",\"assignee\":\" bob \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Hi"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.assignee").value("bob"));
    }

    @Test
    void trimsUnicodeWhitespaceToo() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\u2003Hi\u2003\",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Hi"));
    }

    @Test
    void blankAssigneeIsStoredAsNull() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"description\":\"d\",\"assignee\":\"   \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee").doesNotExist());
    }

    @Test
    void rejectsMissingAndBlankRequiredFieldsWithFieldErrors() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')]").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='description')]").isNotEmpty());
        assertThat(tickets.count()).isZero();
    }

    @Test
    void rejectsTitleOver120AndDescriptionOver5000() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(json("x".repeat(121), "y".repeat(5001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')]").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='description')]").isNotEmpty());
    }

    @Test
    void acceptsTitleOfExactly120Characters() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(json("x".repeat(120), "d")))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\":\"t\",\"description\":\"d\",\"priority\":\"URGENT\"}",
            "{\"title\":\"t\",\"description\":\"d\",\"status\":\"CLOSED\"}",
            "{\"title\":\"t\",\"description\":",
            ""})
    void rejectsInvalidPriorityUnknownFieldAndMalformedJson(String body) throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertThat(tickets.count()).isZero();
    }

    @Test
    void getReturnsDetailIncludingDescriptionAndVersion() throws Exception {
        Ticket t = seed("Login fails", "500 on submit", TicketStatus.OPEN);
        mvc.perform(get("/api/tickets/{id}", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(t.getId()))
                .andExpect(jsonPath("$.description").value("500 on submit"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.priority").value(TicketPriority.MEDIUM.name()));
    }

    @Test
    void getUnknownTicketReturns404() throws Exception {
        mvc.perform(get("/api/tickets/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void getWithNonNumericIdReturns400() throws Exception {
        mvc.perform(get("/api/tickets/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
