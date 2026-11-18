package com.tothenew.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketPriority;
import com.tothenew.tickets.domain.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;

/** FR-4. */
class TicketUpdateTest extends AbstractApiTest {

    private org.springframework.test.web.servlet.ResultActions patchTicket(long id, String body) throws Exception {
        return mvc.perform(patch("/api/tickets/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void updatesAllEditableFields() throws Exception {
        Ticket t = seed("Old", "Old desc", TicketStatus.OPEN);
        patchTicket(t.getId(), "{\"title\":\"New\",\"description\":\"New desc\",\"priority\":\"HIGH\",\"assignee\":\"alice\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"))
                .andExpect(jsonPath("$.description").value("New desc"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignee").value("alice"));

        Ticket saved = tickets.findById(t.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("New");
        assertThat(saved.getAssignee()).isEqualTo("alice");
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void partialUpdateLeavesOtherFieldsUntouched() throws Exception {
        Ticket t = seed("Keep", "Keep desc", TicketStatus.OPEN);
        patchTicket(t.getId(), "{\"priority\":\"LOW\"}").andExpect(status().isOk());

        Ticket saved = tickets.findById(t.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("Keep");
        assertThat(saved.getDescription()).isEqualTo("Keep desc");
        assertThat(saved.getPriority()).isEqualTo(TicketPriority.LOW);
    }

    @Test
    void changesAndThenClearsAssignee() throws Exception {
        Ticket t = seed(TicketStatus.IN_PROGRESS);
        patchTicket(t.getId(), "{\"assignee\":\"bob\"}")
                .andExpect(jsonPath("$.assignee").value("bob"));
        patchTicket(t.getId(), "{\"assignee\":\"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").doesNotExist());
        assertThat(tickets.findById(t.getId()).orElseThrow().getAssignee()).isNull();
    }

    @Test
    void updateBumpsUpdatedAtAndVersion() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        java.time.Instant past = backdate(t.getId());
        patchTicket(t.getId(), "{\"title\":\"Changed\"}").andExpect(jsonPath("$.version").value(1));
        Ticket saved = tickets.findById(t.getId()).orElseThrow();
        assertThat(saved.getUpdatedAt()).isAfter(past);
    }

    @Test
    void emptyPatchIsRejected() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        patchTicket(t.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void statusInPatchIsRejectedAndStatusUnchanged() throws Exception {
        Ticket t = seed(TicketStatus.OPEN);
        patchTicket(t.getId(), "{\"status\":\"CLOSED\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertThat(tickets.findById(t.getId()).orElseThrow().getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void rejectsBlankTitleAndOversizeValues() throws Exception {
        Ticket t = seed("Original", "d", TicketStatus.OPEN);
        patchTicket(t.getId(), "{\"title\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
        patchTicket(t.getId(), "{\"assignee\":\"" + "a".repeat(101) + "\"}")
                .andExpect(status().isBadRequest());
        patchTicket(t.getId(), "{\"priority\":\"URGENT\"}")
                .andExpect(status().isBadRequest());
        assertThat(tickets.findById(t.getId()).orElseThrow().getTitle()).isEqualTo("Original");
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"CLOSED", "CANCELLED"})
    void terminalTicketsCannotBeEdited(TicketStatus terminal) throws Exception {
        Ticket t = seed("Frozen", "d", terminal);
        patchTicket(t.getId(), "{\"title\":\"Hacked\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_EDITABLE"));
        assertThat(tickets.findById(t.getId()).orElseThrow().getTitle()).isEqualTo("Frozen");
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"OPEN", "IN_PROGRESS", "RESOLVED"})
    void nonTerminalTicketsCanBeEdited(TicketStatus status) throws Exception {
        Ticket t = seed(status);
        patchTicket(t.getId(), "{\"title\":\"Edited\"}").andExpect(status().isOk());
    }

    @Test
    void updateUnknownTicketReturns404() throws Exception {
        patchTicket(424242, "{\"title\":\"x\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }
}
