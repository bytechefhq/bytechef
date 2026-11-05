/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ConnectedUserWorkflowReferenceAdminFacadeImpl
    implements ConnectedUserWorkflowReferenceAdminFacade {

    private static final String CONNECTED_USER_VIEW = "CONNECTED_USER_VIEW";

    private final ConnectedUserWorkflowReferenceReader connectedUserWorkflowReferenceReader;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowReferenceAdminFacadeImpl(
        ConnectedUserWorkflowReferenceReader connectedUserWorkflowReferenceReader,
        @Lazy PermissionService permissionService) {

        this.connectedUserWorkflowReferenceReader = connectedUserWorkflowReferenceReader;
        this.permissionService = permissionService;
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('CONNECTED_USER_VIEW')")
    public List<ConnectedUserWorkflowReferenceDTO> getReferences(Set<String> automationWorkflowUuids) {
        Map<String, Boolean> canViewByEnvironment = new HashMap<>();

        return connectedUserWorkflowReferenceReader.getReferences(automationWorkflowUuids)
            .stream()
            .filter(reference -> canViewByEnvironment.computeIfAbsent(
                reference.environment(),
                environment -> permissionService.hasEmbeddedScope(
                    CONNECTED_USER_VIEW, Environment.valueOf(environment))))
            .toList();
    }
}
