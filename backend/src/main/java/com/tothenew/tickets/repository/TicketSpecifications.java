package com.tothenew.tickets.repository;

import com.tothenew.tickets.domain.Ticket;
import com.tothenew.tickets.domain.TicketStatus;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecifications {

    private static final char ESCAPE = '\\';

    private TicketSpecifications() {
    }

    public static Specification<Ticket> filter(TicketStatus status, String keyword) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, ESCAPE),
                        cb.like(cb.lower(root.get("description")), pattern, ESCAPE)));
            }
            return predicate;
        };
    }

    /** Makes user-supplied %, _ and \ match literally. */
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
