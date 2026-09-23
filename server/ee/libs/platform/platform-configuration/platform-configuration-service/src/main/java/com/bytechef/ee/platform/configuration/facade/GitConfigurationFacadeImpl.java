/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class GitConfigurationFacadeImpl implements GitConfigurationFacade {

    private static final String GIT_CONFIGURATION = "git.configuration";

    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public GitConfigurationFacadeImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // The workspace's repository URL and username, read by the settings page. WORKSPACE_MANAGE because whoever can see
    // and change this decides where every project's workflows are pushed.
    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MANAGE')")
    @Transactional(readOnly = true)
    public Optional<GitConfigurationDTO> fetchGitConfiguration(long workspaceId) {
        return propertyService.fetchProperty(GIT_CONFIGURATION, Scope.WORKSPACE, workspaceId)
            .map(property -> ConvertUtils.convertValue(property.getValue(), GitConfigurationDTO.class));
    }

    // Deliberately unguarded, and not reachable from a controller. Its callers are the project push, pull and
    // remote-branch paths and the publish-time Git sync listener, each gated on its own scope. A WORKSPACE_MANAGE guard
    // here would deny an EDITOR every push, because pushing needs the stored credentials to reach the repository.
    @Override
    @Transactional(readOnly = true)
    public GitConfigurationDTO getGitConfiguration(long workspaceId) {
        return fetchGitConfiguration(workspaceId)
            .orElseThrow(() -> new RuntimeException("Git configuration not found"));
    }

    // Was unguarded: any authenticated caller could point a workspace at a repository they control, together with the
    // credentials to reach it, and the next push would send that workspace's workflows there.
    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MANAGE')")
    public void save(GitConfigurationDTO gitConfigurationDTO, long workspaceId) {
        fetchGitConfiguration(workspaceId).ifPresentOrElse(
            curGitConfigurationDTO -> {
                Map<String, Object> map = new HashMap<>(
                    ConvertUtils.convertValue(gitConfigurationDTO, new TypeReference<>() {}));

                if (gitConfigurationDTO.password() == null) {
                    map.put("password", curGitConfigurationDTO.password());
                } else {
                    map.put("password", gitConfigurationDTO.password());
                }

                propertyService.save(GIT_CONFIGURATION, map, Scope.WORKSPACE, workspaceId);
            },
            () -> propertyService.save(
                GIT_CONFIGURATION, ConvertUtils.convertValue(gitConfigurationDTO, new TypeReference<>() {}),
                Scope.WORKSPACE, workspaceId));
    }
}
