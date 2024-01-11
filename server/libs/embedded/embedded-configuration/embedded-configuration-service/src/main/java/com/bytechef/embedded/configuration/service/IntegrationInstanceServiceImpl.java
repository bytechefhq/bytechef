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
import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.repository.IntegrationInstanceRepository;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceServiceImpl implements IntegrationInstanceService {

    private final IntegrationInstanceRepository integrationInstanceRepository;

    public IntegrationInstanceServiceImpl(IntegrationInstanceRepository integrationInstanceRepository) {

        this.integrationInstanceRepository = integrationInstanceRepository;
    }

    @Override
    public IntegrationInstance create(IntegrationInstance integrationInstance) {
        Validate.notNull(integrationInstance, "'integrationInstance' must not be null");
        Validate.isTrue(integrationInstance.getId() == null, "'id' must be null");
        Validate.notNull(integrationInstance.getIntegrationId(), "'integrationId' must not be null");
        Validate.notNull(integrationInstance.getName(), "'integrationId' must not be null");

        integrationInstance.setEnabled(false);

        return integrationInstanceRepository.save(integrationInstance);
    }

    @Override
    public void delete(long id) {
        integrationInstanceRepository.deleteById(id);
    }

    @Override
    public boolean isIntegrationInstanceEnabled(long integrationInstanceId) {
        IntegrationInstance integrationInstance = getIntegrationInstance(integrationInstanceId);

        return integrationInstance.isEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstance getIntegrationInstance(long id) {
        return OptionalUtils.get(integrationInstanceRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getIntegrationIds() {
        return integrationInstanceRepository.findAllIntegrationId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstance> getIntegrationInstances() {
        return getIntegrationInstances(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstance> getIntegrationInstances(Long integrationId, Long tagId) {
        Iterable<IntegrationInstance> integrationInstanceIterable;

        if (integrationId == null && tagId == null) {
            integrationInstanceIterable = integrationInstanceRepository.findAll(Sort.by("name"));
        } else if (integrationId != null && tagId == null) {
            integrationInstanceIterable =
                integrationInstanceRepository.findAllByIntegrationIdOrderByName(integrationId);
        } else if (integrationId == null) {
            integrationInstanceIterable = integrationInstanceRepository.findAllByTagIdOrderByName(tagId);
        } else {
            integrationInstanceIterable =
                integrationInstanceRepository.findAllByIntegrationIdAndTagIdOrderByName(integrationId, tagId);
        }

        return com.bytechef.commons.util.CollectionUtils.toList(integrationInstanceIterable);
    }

    @Override
    public IntegrationInstance update(long id, List<Long> tagIds) {
        IntegrationInstance integrationInstance = getIntegrationInstance(id);

        integrationInstance.setTagIds(tagIds);

        return integrationInstanceRepository.save(integrationInstance);
    }

    @Override
    public IntegrationInstance update(IntegrationInstance integrationInstance) {
        Validate.notNull(integrationInstance, "'integrationInstance' must not be null");
        Validate.notNull(integrationInstance.getIntegrationId(), "'integrationId' must not be null");
        Validate.notNull(integrationInstance.getName(), "'integrationId' must not be null");

        IntegrationInstance curIntegrationInstance =
            getIntegrationInstance(Validate.notNull(integrationInstance.getId(), "id"));

        curIntegrationInstance.setDescription(integrationInstance.getDescription());
        curIntegrationInstance.setName(integrationInstance.getName());
        curIntegrationInstance.setEnabled(integrationInstance.isEnabled());
        curIntegrationInstance.setTagIds(integrationInstance.getTagIds());
        curIntegrationInstance.setVersion(integrationInstance.getVersion());

        return integrationInstanceRepository.save(curIntegrationInstance);
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        IntegrationInstance integrationInstance = getIntegrationInstance(id);

        integrationInstance.setEnabled(enabled);

        integrationInstanceRepository.save(integrationInstance);
    }
}
