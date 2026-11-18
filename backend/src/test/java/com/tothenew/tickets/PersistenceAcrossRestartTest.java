package com.tothenew.tickets;

import static org.assertj.core.api.Assertions.assertThat;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketPriority;
import com.tothenew.tickets.repository.TicketRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** FR-12: data written by one application run is visible after a full shutdown and restart. */
class PersistenceAcrossRestartTest {

    @TempDir
    Path dir;

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(TicketsApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.datasource.url=jdbc:h2:file:" + dir.resolve("tickets")
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
                .run();
    }

    @Test
    void ticketSurvivesRestartOfTheFileBackedDatabase() {
        long id;
        try (ConfigurableApplicationContext first = start()) {
            Ticket t = new Ticket("Survives restart", "persisted", TicketPriority.HIGH, "carol");
            id = first.getBean(TicketRepository.class).saveAndFlush(t).getId();
        }

        try (ConfigurableApplicationContext second = start()) {
            Ticket reloaded = second.getBean(TicketRepository.class).findById(id).orElseThrow();
            assertThat(reloaded.getTitle()).isEqualTo("Survives restart");
            assertThat(reloaded.getPriority()).isEqualTo(TicketPriority.HIGH);
            assertThat(reloaded.getAssignee()).isEqualTo("carol");
        }
    }
}
