package com.example.contracts.approval;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_histories")
public class ApprovalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private String action;

    @Column(name = "comment_text")
    private String commentText;

    @Column(name = "acted_at", nullable = false)
    private OffsetDateTime actedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public Long getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getCommentText() { return commentText; }
    public OffsetDateTime getActedAt() { return actedAt; }

    public static ApprovalHistory of(Long requestId, Long actorId, String action, String comment) {
        ApprovalHistory h = new ApprovalHistory();
        h.requestId = requestId;
        h.actorId = actorId;
        h.action = action;
        h.commentText = comment;
        return h;
    }
}
