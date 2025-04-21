/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.repository.CustomComponentRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class CustomComponentServiceImpl implements CustomComponentService {

    private final CustomComponentRepository customComponentRepository;

    public CustomComponentServiceImpl(CustomComponentRepository customComponentRepository) {
        this.customComponentRepository = customComponentRepository;
    }

    @Override
    public CustomComponent create(CustomComponent customComponent) {
        Assert.notNull(customComponent, "'customComponent' must not be null");
        Assert.isTrue(customComponent.getId() == null, "'id' must be null");
        Assert.notNull(customComponent.getName(), "'componentName' must not be null");

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
    public CustomComponent update(CustomComponent customComponent) {
        Assert.notNull(customComponent, "'customComponent' must not be null");
        Assert.notNull(customComponent.getId(), "id");

        CustomComponent curCustomComponent = getCustomComponent(customComponent.getId());

        curCustomComponent.setDescription(customComponent.getDescription());
        curCustomComponent.setIcon(customComponent.getIcon());
        curCustomComponent.setTitle(customComponent.getTitle());

        return customComponentRepository.save(curCustomComponent);
    }
}
