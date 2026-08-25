/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.service.VariableService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing embedded {@link Variable}s for the tenant admin console.
 *
 * <p>
 * {@link VariableService} is deliberately not authorization-aware -- it is also called by the runtime resolver, which
 * has no security context -- so the {@code @PreAuthorize} expressions here ARE the access control for this admin
 * surface. Unlike the workspace controller, embedded variables have no per-workspace membership to check against, so
 * reads only require authentication and mutations require the tenant admin authority.
 *
 * <p>
 * Returns are mapped to a controller-local {@link VariableResponse} rather than {@link Variable} directly:
 * graphql-java's default {@code String} scalar coercion throws {@code CoercingSerializeException} for anything other
 * than String/numeric/Boolean/UUID, and {@code createdDate}/{@code lastModifiedDate} are {@link java.time.Instant}.
 * Matches the existing {@code OrganizationConnectionGraphQlController} convention.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class EmbeddedVariableGraphQlController {

    private final VariableService variableService;

    @SuppressFBWarnings("EI")
    EmbeddedVariableGraphQlController(VariableService variableService) {
        this.variableService = variableService;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<VariableResponse> embeddedVariables(@Argument long environmentId) {
        return variableService.getVariables(VariableScope.embedded(), environmentId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public VariableResponse createEmbeddedVariable(@Argument long environmentId, @Argument VariableInput input) {
        return toResponse(
            variableService.create(VariableScope.embedded(), environmentId, input.name(), input.value()));
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public VariableResponse updateEmbeddedVariable(
        @Argument long environmentId, @Argument long id, @Argument VariableInput input) {

        return toResponse(
            variableService.update(VariableScope.embedded(), environmentId, id, input.name(), input.value()));
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public boolean deleteEmbeddedVariable(@Argument long environmentId, @Argument long id) {
        variableService.delete(VariableScope.embedded(), environmentId, id);

        return true;
    }

    private VariableResponse toResponse(Variable variable) {
        Instant createdDate = variable.createdDate();
        Instant lastModifiedDate = variable.lastModifiedDate();

        return new VariableResponse(
            variable.id(), variable.name(), variable.value(), variable.environmentId(), variable.createdBy(),
            createdDate == null ? null : createdDate.toString(), variable.lastModifiedBy(),
            lastModifiedDate == null ? null : lastModifiedDate.toString());
    }

    public record VariableInput(String name, String value) {
    }

    public record VariableResponse(
        long id, String name, String value, long environmentId, String createdBy, String createdDate,
        String lastModifiedBy, String lastModifiedDate) {
    }
}
