/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.audit.AppEventAuditEvents;
import com.bytechef.ee.embedded.configuration.audit.AppEventAuditMapper;
import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
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
    @Audited(event = AppEventAuditEvents.APP_EVENT_CREATED, data = {
        @AuditData(key = "appEventId", value = "#result?.id"),
        @AuditData(key = "name", value = "#result?.name")
    })
    @PreAuthorize("isTenantAdmin()")
    @Transactional
    public AppEvent create(AppEvent appEvent) {
        return appEventService.create(appEvent);
    }

    @Override
    @Audited(event = AppEventAuditEvents.APP_EVENT_DELETED, mapper = AppEventAuditMapper.class, data = {
        @AuditData(key = "appEventId", value = "#id")
    })
    @PreAuthorize("isTenantAdmin()")
    @Transactional
    public boolean delete(long id) {
        return appEventService.delete(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public AppEvent getAppEvent(long id) {
        return appEventService.getAppEvent(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<AppEvent> getAppEvents() {
        return appEventService.getAppEvents();
    }

    @Override
    @Audited(event = AppEventAuditEvents.APP_EVENT_UPDATED, data = {
        @AuditData(key = "appEventId", value = "#appEvent.id"),
        @AuditData(key = "name", value = "#appEvent.name")
    })
    @PreAuthorize("isTenantAdmin()")
    @Transactional
    public AppEvent update(AppEvent appEvent) {
        return appEventService.update(appEvent);
    }
}
