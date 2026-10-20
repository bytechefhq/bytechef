/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional(readOnly = true)
public class AppEventFacadeImpl implements AppEventFacade {

    private final AppEventService appEventService;

    @SuppressFBWarnings("EI")
    public AppEventFacadeImpl(AppEventService appEventService) {
        this.appEventService = appEventService;
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('APP_EVENT_CREATE')")
    @Transactional
    public AppEvent create(AppEvent appEvent) {
        return appEventService.create(appEvent);
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('APP_EVENT_DELETE')")
    @Transactional
    public void delete(long id) {
        appEventService.delete(id);
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('APP_EVENT_VIEW')")
    public AppEvent getAppEvent(long id) {
        return appEventService.getAppEvent(id);
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('APP_EVENT_VIEW')")
    public List<AppEvent> getAppEvents() {
        return appEventService.getAppEvents();
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('APP_EVENT_EDIT')")
    @Transactional
    public AppEvent update(AppEvent appEvent) {
        return appEventService.update(appEvent);
    }
}
