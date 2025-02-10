/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.PropertyService;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(readOnly = true)
    public Optional<GitConfigurationDTO> fetchGitConfiguration() {
        return propertyService.fetchProperty(GIT_CONFIGURATION)
            .map(property -> ConvertUtils.convertValue(property.getValue(), GitConfigurationDTO.class));
    }

    @Override
    @Transactional(readOnly = true)
    public GitConfigurationDTO getGitConfiguration() {
        return fetchGitConfiguration()
            .orElseThrow(() -> new RuntimeException("Git configuration not found"));
    }

    @Override
    public void save(GitConfigurationDTO gitConfigurationDTO) {
        fetchGitConfiguration().ifPresentOrElse(
            curGitConfigurationDTO -> {
                Map<String, Object> map = new HashMap<>(
                    ConvertUtils.convertValue(curGitConfigurationDTO, new TypeReference<>() {}));

                if (gitConfigurationDTO.password() == null) {
                    map.put("password", curGitConfigurationDTO.password());
                } else {
                    map.put("password", gitConfigurationDTO.password());
                }

                propertyService.save(GIT_CONFIGURATION, map);
            },
            () -> propertyService.save(
                GIT_CONFIGURATION, ConvertUtils.convertValue(gitConfigurationDTO, new TypeReference<>() {})));
    }
}
