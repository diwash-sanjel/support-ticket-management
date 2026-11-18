package com.tothenew.tickets.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tothenew.tickets.domain.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FR-2, FR-6, FR-7. */
class TicketListSearchTest extends AbstractApiTest {

    @BeforeEach
    void data() {
        seed("Printer jam", "Paper stuck in tray", TicketStatus.OPEN);
        seed("Login error", "User cannot sign in", TicketStatus.IN_PROGRESS);
        seed("Discount 100% off", "Coupon bug", TicketStatus.OPEN);
        seed("Under_score name", "plain", TicketStatus.CLOSED);
    }

    @Test
    void listsAllNewestFirstWithPageMetadata() throws Exception {
        mvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Under_score name"))
                .andExpect(jsonPath("$.content[3].title").value("Printer jam"))
                .andExpect(jsonPath("$.content[0].description").doesNotExist());
    }

    @Test
    void paginates() throws Exception {
        mvc.perform(get("/api/tickets").param("size", "3").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Printer jam"));
    }

    @Test
    void filtersByStatus() throws Exception {
        mvc.perform(get("/api/tickets").param("status", "OPEN"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.status!='OPEN')]").isEmpty());
    }

    @Test
    void searchMatchesTitleCaseInsensitively() throws Exception {
        mvc.perform(get("/api/tickets").param("q", "PRINTER"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Printer jam"));
    }

    @Test
    void searchMatchesDescription() throws Exception {
        mvc.perform(get("/api/tickets").param("q", "sign in"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Login error"));
    }

    @Test
    void searchAndStatusCombineWithAnd() throws Exception {
        mvc.perform(get("/api/tickets").param("q", "e").param("status", "CLOSED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Under_score name"));
    }

    @Test
    void percentSignIsLiteralNotWildcard() throws Exception {
        mvc.perform(get("/api/tickets").param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Discount 100% off"));
    }

    @Test
    void underscoreIsLiteralNotWildcard() throws Exception {
        mvc.perform(get("/api/tickets").param("q", "r_s"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Under_score name"));
        mvc.perform(get("/api/tickets").param("q", "pr_nter"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void blankKeywordIsIgnored() throws Exception {
        mvc.perform(get("/api/tickets").param("q", "   "))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void noMatchReturnsEmptyPage() throws Exception {
        mvc.perform(get("/api/tickets").param("q", "zzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsInvalidStatusAndPagingParameters() throws Exception {
        mvc.perform(get("/api/tickets").param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
        mvc.perform(get("/api/tickets").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/tickets").param("size", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/tickets").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/tickets").param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
