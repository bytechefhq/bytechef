/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.mcp;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.platform.ai.guardrails.McpOutboundRedactor;
import com.bytechef.platform.ai.guardrails.McpOutboundRedactorProvider;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.tokenization.SensitiveDataPolicy;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class McpOutboundRedactorProviderImpl implements McpOutboundRedactorProvider {

    private final AiGuardrails aiGuardrails;
    private final SensitiveDataRedactor sensitiveDataRedactor;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public McpOutboundRedactorProviderImpl(
        AiGuardrails aiGuardrails, SensitiveDataRedactor sensitiveDataRedactor,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.aiGuardrails = aiGuardrails;
        this.sensitiveDataRedactor = sensitiveDataRedactor;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    public Optional<McpOutboundRedactor> fetchRedactor(@Nullable Long workspaceId, String surface) {
        SensitiveDataPolicy policy = SURFACE_EMBEDDED.equals(surface)
            ? aiGuardrails.resolveEmbeddedMcpOutboundPolicy()
            : aiGuardrails.resolveMcpOutboundPolicy(AiGuardrailsSettingsTarget.resolve(null, workspaceId));

        if (policy == null) {
            return Optional.empty();
        }

        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistryProvider.getIfAvailable(), surface);

        return Optional.of(
            serializedResult -> sensitiveDataRedactor.redact(
                serializedResult, policy.kinds(), policy.minConfidence(), metrics));
    }
}
