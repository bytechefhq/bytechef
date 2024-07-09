/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceConfigurationServiceImpl implements IntegrationInstanceConfigurationService {

    private final IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    public IntegrationInstanceConfigurationServiceImpl(
        IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository) {

        this.integrationInstanceConfigurationRepository = integrationInstanceConfigurationRepository;
    }

    @Override
    public IntegrationInstanceConfiguration create(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        Validate.notNull(integrationInstanceConfiguration, "'integrationInstance' must not be null");
        Validate.isTrue(integrationInstanceConfiguration.getId() == null, "'id' must be null");
        Validate.notNull(integrationInstanceConfiguration.getIntegrationId(), "'integrationId' must not be null");

        integrationInstanceConfiguration.setEnabled(false);

        return integrationInstanceConfigurationRepository.save(integrationInstanceConfiguration);
    }

    @Override
    public void delete(long id) {
        integrationInstanceConfigurationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstanceConfiguration getIntegrationInstanceConfiguration(long id) {
        return OptionalUtils.get(integrationInstanceConfigurationRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getIntegrationIds() {
        return integrationInstanceConfigurationRepository.findAllIntegrationIds();
    }

    @Override
    public List<Long> getIntegrationIds(Environment environment) {
        return integrationInstanceConfigurationRepository.findAllIntegrationIdsByEnvironmentAndEnabled(
            environment.ordinal(), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations() {
        return getIntegrationInstanceConfigurations(null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(long integrationId) {
        return getIntegrationInstanceConfigurations(null, integrationId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return integrationInstanceConfigurationRepository.findAllIntegrationInstanceConfigurationsByIdIn(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(
        Environment environment, Long integrationId, Long tagId) {

        return integrationInstanceConfigurationRepository.findAllIntegrationInstanceConfigurations(
            environment == null ? null : environment.ordinal(), integrationId, tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isIntegrationInstanceConfigurationEnabled(long integrationInstanceConfigurationId) {
        return integrationInstanceConfigurationRepository.findById(integrationInstanceConfigurationId)
            .map(integrationInstanceConfiguration -> integrationInstanceConfiguration.isEnabled())
            .orElse(false);
    }

    @Override
    public IntegrationInstanceConfiguration update(long id, List<Long> tagIds) {
        IntegrationInstanceConfiguration integrationInstance = getIntegrationInstanceConfiguration(id);

        integrationInstance.setTagIds(tagIds);

        return integrationInstanceConfigurationRepository.save(integrationInstance);
    }

    @Override
    public IntegrationInstanceConfiguration update(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        Validate.notNull(integrationInstanceConfiguration, "'integrationInstance' must not be null");
        Validate.notNull(integrationInstanceConfiguration.getIntegrationId(), "'integrationId' must not be null");

        IntegrationInstanceConfiguration curIntegrationInstanceConfiguration =
            getIntegrationInstanceConfiguration(Validate.notNull(integrationInstanceConfiguration.getId(), "id"));

        curIntegrationInstanceConfiguration.setDescription(integrationInstanceConfiguration.getDescription());
        curIntegrationInstanceConfiguration.setEnabled(integrationInstanceConfiguration.isEnabled());
        curIntegrationInstanceConfiguration.setIntegrationVersion(
            curIntegrationInstanceConfiguration.getIntegrationVersion());
        curIntegrationInstanceConfiguration.setName(integrationInstanceConfiguration.getName());
        curIntegrationInstanceConfiguration.setTagIds(integrationInstanceConfiguration.getTagIds());
        curIntegrationInstanceConfiguration.setVersion(integrationInstanceConfiguration.getVersion());

        return integrationInstanceConfigurationRepository.save(curIntegrationInstanceConfiguration);
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = getIntegrationInstanceConfiguration(id);

        integrationInstanceConfiguration.setEnabled(enabled);

        integrationInstanceConfigurationRepository.save(integrationInstanceConfiguration);
    }
}
