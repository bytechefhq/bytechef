/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.usage.ConnectionUsageChecker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reports every app that binds a connection, whatever the app's status, so a bound connection cannot be deleted from
 * under it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppConnectionUsageChecker implements ConnectionUsageChecker {

    private final AppConnectionService appConnectionService;
    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppConnectionUsageChecker(AppConnectionService appConnectionService, AppService appService) {
        this.appConnectionService = appConnectionService;
        this.appService = appService;
    }

    @Override
    public List<String> describeUsages(long connectionId) {
        return appConnectionService.getBindingsByConnectionId(connectionId)
            .stream()
            .map(binding -> "app '%s' (binding '%s')".formatted(
                appService.fetchApp(binding.getAppId())
                    .map(App::getName)
                    .orElse("#" + binding.getAppId()),
                binding.getBindingKey()))
            .toList();
    }
}
