/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleService;
import com.bytechef.ee.ai.hub.audit.AiHubAuditMapper;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@Import({
    AiHubIntTestConfiguration.class, AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class,
    AuditMapperResolver.class
})
public class AiHubAuditIntTestConfiguration {

    @Bean
    AiHubAuditMapper aiHubAuditMapper(AiHubChatService chatService, AiHubToolApprovalRuleService ruleService) {
        return new AiHubAuditMapper(chatService, ruleService);
    }

    @Bean
    ApplicationProperties applicationProperties() {
        return new ApplicationProperties();
    }
}
