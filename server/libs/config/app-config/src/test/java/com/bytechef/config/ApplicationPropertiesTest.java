/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties.Ai.Memory.Redis;
import com.bytechef.config.ApplicationProperties.Oauth2;
import com.bytechef.config.ApplicationProperties.Oauth2.ResourceServer;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * @author Ivica Cardic
 */
class ApplicationPropertiesTest {

    @Test
    void testBindsTheAiMemoryRedisSettingsWithoutUnknownProperties() {
        MapConfigurationPropertySource mapConfigurationPropertySource = new MapConfigurationPropertySource(
            Map.of(
                "bytechef.ai.memory.provider", "redis",
                "bytechef.ai.memory.redis.host", "redis.internal",
                "bytechef.ai.memory.redis.port", "6380",
                "bytechef.ai.memory.redis.username", "bytechef",
                "bytechef.ai.memory.redis.password", "secret",
                "bytechef.ai.memory.redis.session-key-prefix", "session:",
                "bytechef.ai.memory.session-max-archived-events", "250"));

        Binder binder = new Binder(mapConfigurationPropertySource);

        ApplicationProperties applicationProperties = binder.bind(
            "bytechef", Bindable.of(ApplicationProperties.class), new NoUnboundElementsBindHandler(BindHandler.DEFAULT))
            .get();

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Memory memory = ai.getMemory();

        Redis redis = memory.getRedis();

        assertThat(memory.getProvider()).isEqualTo(ApplicationProperties.Ai.Memory.Provider.REDIS);
        assertThat(redis.getHost()).isEqualTo("redis.internal");
        assertThat(redis.getPort()).isEqualTo(6380);
        assertThat(redis.getUsername()).isEqualTo("bytechef");
        assertThat(redis.getPassword()).isEqualTo("secret");
        assertThat(redis.getSessionKeyPrefix()).isEqualTo("session:");
        assertThat(memory.getSessionMaxArchivedEvents()).isEqualTo(250);
    }

    @Test
    void testBindsTheApprovalAndExecutionMonitorSettingsWithoutUnknownProperties() {
        Map<String, String> properties = new HashMap<>();

        properties.put("bytechef.approval.signed-token.secret", "active-secret");
        properties.put("bytechef.approval.signed-token.required", "true");
        properties.put("bytechef.approval.signed-token.default-ttl", "P7D");
        properties.put("bytechef.approval.signed-token.clock-skew", "PT30S");
        properties.put("bytechef.approval.signed-token.previous-secrets[0]", "previous-secret-1");
        properties.put("bytechef.approval.signed-token.previous-secrets[1]", "previous-secret-2");
        properties.put("bytechef.automation.approval-task.reconciliation.enabled", "false");
        properties.put("bytechef.webhook.discord.public-key", "discord-public-key");
        properties.put("bytechef.webhook.whatsapp.verify-token", "whatsapp-verify-token");
        properties.put("bytechef.workflow.execution.approval-escalation.enabled", "false");
        properties.put("bytechef.workflow.execution.approval-escalation.after", "PT48H");
        properties.put("bytechef.workflow.execution.approval-expiry.enabled", "false");
        properties.put("bytechef.workflow.execution.approval-reminder.enabled", "false");
        properties.put("bytechef.workflow.execution.approval-reminder.lead-time", "PT12H");

        Binder binder = new Binder(new MapConfigurationPropertySource(properties));

        ApplicationProperties applicationProperties = binder.bind(
            "bytechef", Bindable.of(ApplicationProperties.class), new NoUnboundElementsBindHandler(BindHandler.DEFAULT))
            .get();

        ApplicationProperties.Approval approval = applicationProperties.getApproval();

        ApplicationProperties.Approval.SignedToken signedToken = approval.getSignedToken();

        assertThat(signedToken.getSecret()).isEqualTo("active-secret");
        assertThat(signedToken.isRequired()).isTrue();
        assertThat(signedToken.getDefaultTtl()).isEqualTo(Duration.ofDays(7));
        assertThat(signedToken.getClockSkew()).isEqualTo(Duration.ofSeconds(30));
        assertThat(signedToken.getPreviousSecrets()).containsExactly("previous-secret-1", "previous-secret-2");

        ApplicationProperties.Automation automation = applicationProperties.getAutomation();

        ApplicationProperties.Automation.ApprovalTask approvalTask = automation.getApprovalTask();

        assertThat(approvalTask.getReconciliation()
            .isEnabled()).isFalse();

        ApplicationProperties.Webhook webhook = applicationProperties.getWebhook();

        assertThat(webhook.getDiscord()
            .getPublicKey()).isEqualTo("discord-public-key");
        assertThat(webhook.getWhatsapp()
            .getVerifyToken()).isEqualTo("whatsapp-verify-token");

        ApplicationProperties.Workflow workflow = applicationProperties.getWorkflow();

        ApplicationProperties.Workflow.Execution execution = workflow.getExecution();

        ApplicationProperties.Workflow.Execution.ApprovalEscalation approvalEscalation =
            execution.getApprovalEscalation();

        assertThat(approvalEscalation.isEnabled()).isFalse();
        assertThat(approvalEscalation.getAfter()).isEqualTo(Duration.ofHours(48));

        assertThat(execution.getApprovalExpiry()
            .isEnabled()).isFalse();

        ApplicationProperties.Workflow.Execution.ApprovalReminder approvalReminder = execution.getApprovalReminder();

        assertThat(approvalReminder.isEnabled()).isFalse();
        assertThat(approvalReminder.getLeadTime()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void testLeavesTheOptionalApprovalAndExecutionMonitorSettingsNullUntilBound() {
        ApplicationProperties applicationProperties = bind(
            new MapPropertySource("test", Map.of("bytechef.workflow.execution.approval-expiry.enabled", "true")));

        ApplicationProperties.Workflow workflow = applicationProperties.getWorkflow();

        ApplicationProperties.Workflow.Execution execution = workflow.getExecution();

        assertThat(execution.getApprovalEscalation()
            .getAfter()).isNull();

        ApplicationProperties.Approval approval = applicationProperties.getApproval();

        assertThat(approval.getSignedToken()
            .getPreviousSecrets()).isEmpty();
    }

    @Test
    void testLeavesTheAiMemoryRedisPortNullUntilBound() {
        MapConfigurationPropertySource mapConfigurationPropertySource = new MapConfigurationPropertySource(
            Map.of("bytechef.ai.memory.provider", "redis"));

        Binder binder = new Binder(mapConfigurationPropertySource);

        ApplicationProperties applicationProperties = binder.bind(
            "bytechef", Bindable.of(ApplicationProperties.class))
            .get();

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Memory memory = ai.getMemory();

        Redis redis = memory.getRedis();

        assertThat(redis.getPort()).isNull();
    }

    @Test
    void testResourceServerEmptyByDefault() {
        Oauth2 oauth2 = new Oauth2();

        assertThat(oauth2.getResourceServer()
            .getIssuers()).isEmpty();
    }

    @Test
    void testResourceServerIssuerBinds() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(
            Map.of(
                "bytechef.oauth2.resource-server.issuers[0].uri", "https://as.example.com",
                "bytechef.oauth2.resource-server.issuers[0].tenant-claim", "tenant_id",
                "bytechef.oauth2.resource-server.issuers[1].uri", "https://idp.customer.com",
                "bytechef.oauth2.resource-server.issuers[1].tenant-claim", "org",
                "bytechef.oauth2.resource-server.issuers[1].authorities-claim", "groups",
                "bytechef.oauth2.resource-server.issuers[1].authorities[0]", "ROLE_USER"));

        Oauth2 oauth2 = new Binder(source)
            .bind("bytechef.oauth2", Oauth2.class)
            .get();

        ResourceServer resourceServer = oauth2.getResourceServer();

        assertThat(resourceServer.getIssuers()).hasSize(2);

        ResourceServer.Issuer embeddedIssuer = resourceServer.getIssuers()
            .get(0);

        assertThat(embeddedIssuer.getUri()).isEqualTo("https://as.example.com");
        assertThat(embeddedIssuer.getTenantClaim()).isEqualTo("tenant_id");
        assertThat(embeddedIssuer.getAuthoritiesClaim()).isNull();
        assertThat(embeddedIssuer.getAuthorities()).isEmpty();

        ResourceServer.Issuer externalIssuer = resourceServer.getIssuers()
            .get(1);

        assertThat(externalIssuer.getUri()).isEqualTo("https://idp.customer.com");
        assertThat(externalIssuer.getTenantClaim()).isEqualTo("org");
        assertThat(externalIssuer.getAuthoritiesClaim()).isEqualTo("groups");
        assertThat(externalIssuer.getAuthorities()).isEqualTo(List.of("ROLE_USER"));
    }

    @Test
    void testAllowedParentOriginsDefaultsToEmpty() {
        ApplicationProperties applicationProperties = bind(new MapPropertySource("test", Map.of()));

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        assertThat(embedded.getAllowedParentOrigins()).isEmpty();
    }

    @Test
    void testAllowedParentOriginsBindsFromProperty() {
        ApplicationProperties applicationProperties = bind(
            new MapPropertySource(
                "test",
                Map.of("bytechef.embedded.allowed-parent-origins", "https://a.example,https://b.example")));

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        assertThat(embedded.getAllowedParentOrigins()).containsExactly("https://a.example", "https://b.example");
    }

    @Test
    void testAllowedParentOriginsBindsFromEnvironmentVariable() {
        ApplicationProperties applicationProperties = bind(
            new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                Map.of("BYTECHEF_EMBEDDED_ALLOWED_PARENT_ORIGINS", "https://a.example,https://b.example")));

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        assertThat(embedded.getAllowedParentOrigins()).containsExactly("https://a.example", "https://b.example");
    }

    private static ApplicationProperties bind(PropertySource<?> propertySource) {
        MutablePropertySources propertySources = new MutablePropertySources();

        propertySources.addFirst(propertySource);

        Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));

        return binder
            .bindOrCreate(
                "bytechef", Bindable.of(ApplicationProperties.class),
                new NoUnboundElementsBindHandler(BindHandler.DEFAULT, new UnboundElementsSourceFilter()));
    }
}
