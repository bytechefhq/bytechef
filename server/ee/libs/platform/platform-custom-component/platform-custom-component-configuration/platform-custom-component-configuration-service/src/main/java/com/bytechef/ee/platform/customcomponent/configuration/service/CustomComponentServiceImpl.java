/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.repository.CustomComponentRepository;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
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
        return customComponentRepository.findAll(Sort.by(Sort.Order.asc("name")));
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
