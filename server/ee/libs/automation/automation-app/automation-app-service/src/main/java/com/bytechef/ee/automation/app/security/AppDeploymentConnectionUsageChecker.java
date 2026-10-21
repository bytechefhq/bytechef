/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.usage.ConnectionUsageChecker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reports every app instance that deploys through a connection, whatever its status or its app's, so a deployment
 * connection cannot be deleted from under it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppDeploymentConnectionUsageChecker implements ConnectionUsageChecker {

    private final AppInstanceService appInstanceService;
    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppDeploymentConnectionUsageChecker(AppInstanceService appInstanceService, AppService appService) {
        this.appInstanceService = appInstanceService;
        this.appService = appService;
    }

    @Override
    public List<String> describeUsages(long connectionId) {
        return appInstanceService.getAppInstancesByDeploymentConnectionId(connectionId)
            .stream()
            .map(this::describeUsage)
            .toList();
    }

    private String describeUsage(AppInstance appInstance) {
        String appName = appService.fetchApp(appInstance.getAppId())
            .map(App::getName)
            .orElse("#" + appInstance.getAppId());

        return "app '%s' instance '%s' (deployment)".formatted(appName, appInstance.getName());
    }
}
