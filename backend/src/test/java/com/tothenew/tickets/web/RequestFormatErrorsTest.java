package com.tothenew.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** NFR-1 / FR-10 / FR-11: framework-level request problems must use the common error body and be specific. */
class RequestFormatErrorsTest extends AbstractApiTest {

    @Test
    void wrongContentTypeReturns415NotAnInternalError() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void missingContentTypeReturns415() throws Exception {
        mvc.perform(post("/api/tickets").content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void invalidEnumValueNamesTheFieldAndListsAllowedValues() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"description\":\"d\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("priority"))
                .andExpect(jsonPath("$.fieldErrors[0].message")
                        .value(Matchers.allOf(Matchers.containsString("LOW"), Matchers.containsString("CRITICAL"))));
    }

    @Test
    void invalidTransitionTargetNamesTheField() throws Exception {
        long id = seed(com.tothenew.tickets.domain.TicketStatus.OPEN).getId();
        mvc.perform(post("/api/tickets/{id}/transitions", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("targetStatus"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(Matchers.containsString("CANCELLED")));
    }

    @Test
    void unknownFieldIsNamed() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"description\":\"d\",\"status\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Unknown field"));
    }

    @Test
    void malformedJsonReturns400WithoutFieldErrors() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void invalidStatusFilterListsAllowedValues() throws Exception {
        mvc.perform(get("/api/tickets").param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value(Matchers.containsString("IN_PROGRESS")));
    }

    @Test
    void echoedBadValueIsTruncated() throws Exception {
        String longValue = "A".repeat(500);
        String body = mvc.perform(get("/api/tickets").param("status", longValue))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(longValue);
    }
}
