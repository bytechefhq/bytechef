/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads an App's visibility and creator straight from its row; an unknown id resolves to no record, which callers treat
 * as not visible.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppVisibilityProvider implements ResourceVisibilityProvider {

    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppVisibilityProvider(AppService appService) {
        this.appService = appService;
    }

    @Override
    public String resourceType() {
        return "App";
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return appService.fetchApp(id)
            .map(app -> new VisibilityRecord(app.getId(), app.getVisibility(), app.getCreatedBy()));
    }
}
