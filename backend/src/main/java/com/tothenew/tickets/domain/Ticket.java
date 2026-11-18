package com.tothenew.tickets.domain;

import com.tothenew.tickets.exception.InvalidStatusTransitionException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Column(length = 100)
    private String assignee;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "ticket", fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC, id ASC")
    private List<TicketComment> comments = new ArrayList<>();

    protected Ticket() {
    }

    public Ticket(String title, String description, TicketPriority priority, String assignee) {
        this.title = title;
        this.description = description;
        this.priority = priority == null ? TicketPriority.MEDIUM : priority;
        this.assignee = assignee;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public TicketPriority getPriority() { return priority; }
    public String getAssignee() { return assignee; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<TicketComment> getComments() { return comments; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    /** The only way to change status: rejects anything the state machine does not allow. */
    public void transitionTo(TicketStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        this.status = target;
    }
}
