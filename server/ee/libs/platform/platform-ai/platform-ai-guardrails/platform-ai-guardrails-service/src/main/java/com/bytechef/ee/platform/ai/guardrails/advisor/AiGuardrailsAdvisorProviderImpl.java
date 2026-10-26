/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.workspace.JobPrincipalWorkspaceResolver;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.guardrails.RestorationDestination;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnBean(AiGuardrails.class)
public class AiGuardrailsAdvisorProviderImpl implements AiGuardrailsAdvisorProvider {

    private final AiGuardrails aiGuardrails;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver;

    @SuppressFBWarnings("EI2")
    public AiGuardrailsAdvisorProviderImpl(
        AiGuardrails aiGuardrails, ObjectProvider<MeterRegistry> meterRegistryProvider,
        JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver) {

        this.aiGuardrails = aiGuardrails;
        this.meterRegistryProvider = meterRegistryProvider;
        this.jobPrincipalWorkspaceResolver = jobPrincipalWorkspaceResolver;
    }

    @Override
    public Optional<Advisor> getAdvisor(
        @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface,
        RestorationDestination destination) {

        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(
            platformType, jobPrincipalWorkspaceResolver.resolve(platformType, jobPrincipalId));
        AiGuardrailMetrics metrics = buildMetricsIfActive(target, surface);

        if (metrics == null) {
            return Optional.empty();
        }

        return Optional.of(new AiGuardrailsAdvisor(aiGuardrails, target, metrics, surface, destination));
    }

    @Override
    public @Nullable SensitiveDataMetrics getMetrics(
        @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface) {

        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(
            platformType, jobPrincipalWorkspaceResolver.resolve(platformType, jobPrincipalId));

        return buildMetricsIfActive(target, surface);
    }

    @Override
    public Optional<Advisor> getAdvisorForWorkspace(
        @Nullable Long workspaceId, String surface, RestorationDestination destination) {

        AiGuardrailsSettingsTarget target = AiGuardrailsSettingsTarget.resolve(null, workspaceId);
        AiGuardrailMetrics metrics = buildMetricsIfActive(target, surface);

        if (metrics == null) {
            return Optional.empty();
        }

        return Optional.of(new AiGuardrailsAdvisor(aiGuardrails, target, metrics, surface, destination));
    }

    @Override
    public @Nullable SensitiveDataMetrics getMetricsForWorkspace(@Nullable Long workspaceId, String surface) {
        return buildMetricsIfActive(AiGuardrailsSettingsTarget.resolve(null, workspaceId), surface);
    }

    private @Nullable AiGuardrailMetrics buildMetricsIfActive(AiGuardrailsSettingsTarget target, String surface) {
        if (!aiGuardrails.isActive(target)) {
            return null;
        }

        return new AiGuardrailMetrics(meterRegistryProvider.getIfAvailable(), surface);
    }
}
