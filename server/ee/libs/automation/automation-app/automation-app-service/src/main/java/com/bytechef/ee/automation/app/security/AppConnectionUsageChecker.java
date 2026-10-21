/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.usage.ConnectionUsageChecker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reports every app instance that binds a connection, whatever its status or its app's, so a bound connection cannot be
 * deleted from under it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppConnectionUsageChecker implements ConnectionUsageChecker {

    private final AppConnectionService appConnectionService;
    private final AppInstanceService appInstanceService;
    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppConnectionUsageChecker(
        AppConnectionService appConnectionService, AppInstanceService appInstanceService, AppService appService) {

        this.appConnectionService = appConnectionService;
        this.appInstanceService = appInstanceService;
        this.appService = appService;
    }

    @Override
    public List<String> describeUsages(long connectionId) {
        return appConnectionService.getBindingsByConnectionId(connectionId)
            .stream()
            .map(this::describeUsage)
            .toList();
    }

    private String describeUsage(AppConnection binding) {
        Optional<AppInstance> appInstance = appInstanceService.fetchAppInstance(binding.getAppInstanceId());

        String appName = appInstance.flatMap(instance -> appService.fetchApp(instance.getAppId()))
            .map(App::getName)
            .orElse("#" + binding.getAppInstanceId());
        String appInstanceName = appInstance.map(AppInstance::getName)
            .orElse("#" + binding.getAppInstanceId());

        return "app '%s' instance '%s' (binding '%s')".formatted(appName, appInstanceName, binding.getBindingKey());
    }
}
