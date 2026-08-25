/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.resolver;

import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.provider.VariableScopeProvider;
import com.bytechef.ee.platform.variable.service.VariableService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.variable.WorkflowVariablesResolver;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * EE implementation of the CE {@link WorkflowVariablesResolver} seam. Dispatches on {@link PlatformType} to the
 * matching {@link VariableScopeProvider}, resolves the environment id and delegates to {@link VariableService}.
 * <p>
 * Fail-open by contract: any failure -- including the distributed apps' remote {@code PropertyService} client, which
 * throws {@code UnsupportedOperationException} on every method since variables are monolith-only in v1 -- yields an
 * empty map and never propagates. The first failure is logged at WARN; every subsequent failure in this JVM is logged
 * at DEBUG only, so a persistently unavailable variable store does not flood the logs of deployments that legitimately
 * cannot serve variables.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class WorkflowVariablesResolverImpl implements WorkflowVariablesResolver {

    private static final Logger log = LoggerFactory.getLogger(WorkflowVariablesResolverImpl.class);

    private final AtomicBoolean failureLogged = new AtomicBoolean();
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final VariableService variableService;
    private final Map<PlatformType, VariableScopeProvider> variableScopeProviderMap;

    @SuppressFBWarnings("EI")
    public WorkflowVariablesResolverImpl(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, VariableService variableService,
        List<VariableScopeProvider> variableScopeProviders) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.variableService = variableService;
        this.variableScopeProviderMap = variableScopeProviders.stream()
            .collect(Collectors.toMap(VariableScopeProvider::getType, Function.identity()));
    }

    @Override
    public Map<String, String> resolveForJobPrincipal(long jobPrincipalId, PlatformType type) {
        try {
            VariableScopeProvider variableScopeProvider = variableScopeProviderMap.get(type);

            if (variableScopeProvider == null) {
                return Map.of();
            }

            Optional<VariableScope> variableScope = variableScopeProvider.getVariableScope(jobPrincipalId);

            if (variableScope.isEmpty()) {
                return Map.of();
            }

            JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(type);

            long environmentId = jobPrincipalAccessor.getEnvironmentId(jobPrincipalId);

            return variableService.getVariableMap(variableScope.get(), environmentId);
        } catch (RuntimeException runtimeException) {
            logFailure(runtimeException);

            return Map.of();
        }
    }

    @Override
    public Map<String, String> resolveForWorkflow(String workflowId, long environmentId) {
        try {
            for (VariableScopeProvider variableScopeProvider : variableScopeProviderMap.values()) {
                Optional<VariableScope> variableScope =
                    variableScopeProvider.getVariableScopeByWorkflowId(workflowId);

                if (variableScope.isPresent()) {
                    return variableService.getVariableMap(variableScope.get(), environmentId);
                }
            }

            return Map.of();
        } catch (RuntimeException runtimeException) {
            logFailure(runtimeException);

            return Map.of();
        }
    }

    private void logFailure(RuntimeException runtimeException) {
        if (failureLogged.compareAndSet(false, true)) {
            log.warn(
                "Unable to resolve workflow variables; jobs will run without a 'vars' snapshot. Further failures " +
                    "in this JVM are logged at debug level.",
                runtimeException);
        } else if (log.isDebugEnabled()) {
            log.debug("Unable to resolve workflow variables", runtimeException);
        }
    }
}
