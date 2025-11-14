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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.SharedTemplate;
import com.bytechef.automation.configuration.repository.SharedTemplateRepository;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.util.TenantUtils;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class SharedTemplateServiceImpl implements SharedTemplateService {

    private final SharedTemplateRepository sharedTemplateRepository;

    public SharedTemplateServiceImpl(SharedTemplateRepository sharedTemplateRepository) {
        this.sharedTemplateRepository = sharedTemplateRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SharedTemplate> fetchSharedTemplate(UUID uuid) {
        return TenantUtils.callWithTenantId(
            TenantContext.DEFAULT_TENANT_ID, () -> sharedTemplateRepository.findByUuid(uuid));
    }

    @Override
    @Transactional(readOnly = true)
    public SharedTemplate getSharedTemplate(UUID uuid) {
        return TenantUtils.callWithTenantId(
            TenantContext.DEFAULT_TENANT_ID,
            () -> sharedTemplateRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Shared template not found for uuid: " + uuid)));
    }

    @Override
    public SharedTemplate save(UUID uuid, FileEntry template) {
        Assert.notNull(uuid, "'uuid' must not be null");
        Assert.notNull(template, "'template' must not be null");

        return TenantUtils.callWithTenantId(TenantContext.DEFAULT_TENANT_ID, () -> fetchSharedTemplate(uuid)
            .map(sharedTemplate -> {
                sharedTemplate.setTemplate(template);

                return sharedTemplateRepository.save(sharedTemplate);
            })
            .orElseGet(() -> {
                SharedTemplate newSharedTemplate = new SharedTemplate();

                newSharedTemplate.setUuid(uuid);
                newSharedTemplate.setTemplate(template);

                return sharedTemplateRepository.save(newSharedTemplate);
            }));
    }

    public SharedTemplate update(SharedTemplate sharedTemplate) {
        Assert.notNull(sharedTemplate, "'sharedTemplate' must not be null");
        Assert.notNull(sharedTemplate.getId(), "'id' must not be null");
        Assert.notNull(sharedTemplate.getUuid(), "'uuid' must not be null");

        return TenantUtils.callWithTenantId(TenantContext.DEFAULT_TENANT_ID, () -> {
            SharedTemplate existingTemplate = sharedTemplateRepository.findById(sharedTemplate.getId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Shared template not found for id: " + sharedTemplate.getId()));

            existingTemplate.setTemplate(sharedTemplate.getTemplate());

            return sharedTemplateRepository.save(existingTemplate);
        });
    }
}
