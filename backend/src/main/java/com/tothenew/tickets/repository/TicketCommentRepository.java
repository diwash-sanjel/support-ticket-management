package com.tothenew.tickets.repository;

import com.tothenew.tickets.domain.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
}
