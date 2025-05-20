/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.repository.AppEventRepository;
import java.util.List;
import org.apache.commons.lang3.Validate;
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
public class AppEventServiceImpl implements AppEventService {

    private final AppEventRepository appEventRepository;

    public AppEventServiceImpl(AppEventRepository appEventRepository) {
        this.appEventRepository = appEventRepository;
    }

    @Override
    public AppEvent create(AppEvent appEvent) {
        Assert.notNull(appEvent, "'appEvent' must not be null");
        Assert.isTrue(appEvent.getId() == null, "'id' must be null");
        Assert.notNull(appEvent.getName(), "'name' must not be null");
        Assert.notNull(appEvent.getSchema(), "'schema' must not be null");

        return appEventRepository.save(appEvent);
    }

    @Override
    public void delete(long id) {
        appEventRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AppEvent getAppEvent(long id) {
        return OptionalUtils.get(appEventRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppEvent> getAppEvents() {
        return appEventRepository.findAll(Sort.by(Sort.Order.asc("name")));
    }

    @Override
    public AppEvent update(AppEvent appEvent) {
        Assert.notNull(appEvent, "'appEvent' must not be null");

        AppEvent curAppEvent = getAppEvent(Validate.notNull(appEvent.getId(), "id"));

        curAppEvent.setName(Validate.notNull(appEvent.getName(), "name"));
        curAppEvent.setSchema(appEvent.getSchema());
        curAppEvent.setVersion(appEvent.getVersion());

        return appEventRepository.save(curAppEvent);
    }
}
