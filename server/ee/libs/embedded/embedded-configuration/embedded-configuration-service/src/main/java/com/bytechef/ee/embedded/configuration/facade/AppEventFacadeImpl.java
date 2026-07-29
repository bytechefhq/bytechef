/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
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
@Transactional(readOnly = true)
public class AppEventFacadeImpl implements AppEventFacade {

    private final AppEventService appEventService;

    @SuppressFBWarnings("EI")
    public AppEventFacadeImpl(AppEventService appEventService) {
        this.appEventService = appEventService;
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
}
