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

package com.bytechef.platform.customcomponent.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.platform.customcomponent.configuration.repository.CustomComponentRepository;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class CustomComponentServiceImpl implements CustomComponentService {

    private final CustomComponentRepository customComponentRepository;

    public CustomComponentServiceImpl(CustomComponentRepository customComponentRepository) {
        this.customComponentRepository = customComponentRepository;
    }

    @Override
    public CustomComponent create(@NonNull CustomComponent customComponent) {
        Validate.notNull(customComponent, "'customComponent' must not be null");
        Validate.isTrue(customComponent.getId() == null, "'id' must be null");
        Validate.notNull(customComponent.getName(), "'componentName' must not be null");

        return customComponentRepository.save(customComponent);
    }

    @Override
    public void delete(long id) {
        customComponentRepository.deleteById(id);
    }

    @Override
    public void enableCustomComponent(long id, boolean enable) {
        CustomComponent customComponent = getCustomComponent(id);

        customComponent.setEnabled(enable);

        customComponentRepository.save(customComponent);
    }

    @Override
    public Optional<CustomComponent> fetchCustomComponent(String name, int componentVersion) {
        return customComponentRepository.findByNameAndComponentVersion(name, componentVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomComponent getCustomComponent(long id) {
        return OptionalUtils.get(customComponentRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomComponent> getCustomComponents() {
        return customComponentRepository.findAll();
    }

    @Override
    public CustomComponent update(@NonNull CustomComponent customComponent) {
        Validate.notNull(customComponent, "'customComponent' must not be null");

        CustomComponent curCustomComponent = getCustomComponent(Validate.notNull(customComponent.getId(), "id"));

        curCustomComponent.setDescription(customComponent.getDescription());
        curCustomComponent.setIcon(customComponent.getIcon());
        curCustomComponent.setTitle(customComponent.getTitle());

        return customComponentRepository.save(curCustomComponent);
    }
}
