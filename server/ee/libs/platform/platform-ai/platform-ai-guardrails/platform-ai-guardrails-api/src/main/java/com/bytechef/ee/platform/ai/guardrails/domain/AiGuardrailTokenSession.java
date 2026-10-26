/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.domain;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_guardrail_token_session")
public class AiGuardrailTokenSession {

    private String conversationId;

    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @LastModifiedDate
    private Instant lastModifiedDate;

    private String sessionId;

    private String tokens;

    private long userId;

    private long workspaceId;

    private AiGuardrailTokenSession() {
    }

    public AiGuardrailTokenSession(
        long workspaceId, long userId, String conversationId, String sessionId, String tokens) {

        this.workspaceId = workspaceId;
        this.userId = userId;
        this.conversationId = conversationId;
        this.sessionId = sessionId;
        this.tokens = tokens;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTokens() {
        return tokens;
    }

    public long getUserId() {
        return userId;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setTokens(String tokens) {
        this.tokens = tokens;
    }

    @Override
    public String toString() {
        return "AiGuardrailTokenSession{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", userId=" + userId +
            ", conversationId='" + conversationId + '\'' +
            ", sessionId='" + sessionId + '\'' +
            ", createdDate=" + createdDate +
            '}';
    }
}
