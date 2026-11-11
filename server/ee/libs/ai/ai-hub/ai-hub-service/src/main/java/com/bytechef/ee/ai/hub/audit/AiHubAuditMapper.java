/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalFacade.Resolution;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRule;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleService;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubAuditMapper implements AuditMapper {

    private static final String RULE_ID = "ruleId";

    private final AiHubChatService chatService;
    private final AiHubToolApprovalRuleService ruleService;

    @SuppressFBWarnings("EI")
    public AiHubAuditMapper(AiHubChatService chatService, AiHubToolApprovalRuleService ruleService) {
        this.chatService = chatService;
        this.ruleService = ruleService;
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        return switch (auditInvocation.event()) {
            case AiHubAuditEvents.AI_HUB_CHAT_CREATED -> chatService
                .findByThreadId((String) auditInvocation.requireArgument("threadId"))
                .map(AiHubChat::getId)
                .orElse(null);
            case AiHubAuditEvents.AI_HUB_CHAT_DELETED -> fetchThreadId(
                (Long) auditInvocation.requireArgument("chatId"));
            case AiHubAuditEvents.AI_HUB_TOOL_APPROVAL_RULE_CHANGED -> fetchRule(auditInvocation);
            default -> null;
        };
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        switch (auditInvocation.event()) {
            case AiHubAuditEvents.AI_HUB_CHAT_DELETED -> {
                if (auditInvocation.captured() instanceof String threadId) {
                    data.put("threadId", threadId);
                }
            }
            case AiHubAuditEvents.AI_HUB_TOOL_APPROVAL_DECIDED -> putDecision(data, auditInvocation.result());
            case AiHubAuditEvents.AI_HUB_TOOL_APPROVAL_RULE_CHANGED -> {
                if (auditInvocation.captured() instanceof AiHubToolApprovalRule rule) {
                    data.put("toolName", rule.getToolName());

                    AiHubToolApprovalRule.Mode mode = rule.getMode();

                    data.put("mode", mode.name());
                }
            }
            default -> {
            }
        }

        return data;
    }

    @Override
    public boolean shouldRecord(AuditInvocation auditInvocation) {
        if (!AiHubAuditEvents.AI_HUB_CHAT_CREATED.equals(auditInvocation.event())) {
            return true;
        }

        return auditInvocation.captured() == null || auditInvocation.outcome() != AuditOutcome.SUCCESS;
    }

    private @Nullable String fetchThreadId(long chatId) {
        try {
            return chatService.getThreadId(chatId);
        } catch (NotFoundException notFoundException) {
            return null;
        }
    }

    private @Nullable AiHubToolApprovalRule fetchRule(AuditInvocation auditInvocation) {
        Object ruleId = auditInvocation.argument(RULE_ID);

        if (ruleId == null) {
            return null;
        }

        Optional<AiHubToolApprovalRule> ruleOptional = ruleService
            .getRules((Long) auditInvocation.requireArgument("workspaceId"))
            .stream()
            .filter(rule -> Objects.equals(rule.getId(), ruleId))
            .findFirst();

        return ruleOptional.orElse(null);
    }

    private static void putDecision(Map<String, String> data, @Nullable Object result) {
        if (!(result instanceof Resolution resolution)) {
            return;
        }

        AiHubToolApproval approval = resolution.approval();

        data.put("chatId", String.valueOf(approval.getChatId()));
        data.put("toolName", approval.getToolName());

        String componentName = approval.getComponentName();

        if (componentName != null) {
            data.put("componentName", componentName);
        }

        data.put("requestedByUserId", String.valueOf(approval.getRequestedByUserId()));

        Long decidedByUserId = approval.getDecidedByUserId();

        if (decidedByUserId != null) {
            data.put("decidedByUserId", String.valueOf(decidedByUserId));
        }
    }
}
