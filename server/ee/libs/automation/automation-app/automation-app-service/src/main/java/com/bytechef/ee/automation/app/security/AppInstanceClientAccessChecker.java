/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppKeyAccess;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.oauth2.authorizationserver.authentication.OAuth2ClientAccessChecker;
import com.bytechef.platform.oauth2.authorizationserver.registration.AppClients;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppInstanceClientAccessChecker implements OAuth2ClientAccessChecker {

    private final AppInstanceService appInstanceService;
    private final AppService appService;
    private final PermissionService permissionService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public AppInstanceClientAccessChecker(
        AppInstanceService appInstanceService, AppService appService, PermissionService permissionService,
        UserService userService) {

        this.appInstanceService = appInstanceService;
        this.appService = appService;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @Override
    public boolean supports(RegisteredClient registeredClient) {
        return AppClients.isAppClient(registeredClient);
    }

    @Override
    public boolean isAllowed(RegisteredClient registeredClient, Authentication principal) {
        String clientId = registeredClient.getClientId();

        Optional<AppInstance> appInstanceOptional = fetchAppInstance(clientId);

        if (appInstanceOptional.isEmpty()) {
            return false;
        }

        AppInstance appInstance = appInstanceOptional.get();

        if (appInstance.getUserAuthMode() == AppUserAuthMode.NONE ||
            !Objects.equals(appInstance.getRegisteredClientId(), clientId)) {

            return false;
        }

        App app = appService.getApp(appInstance.getAppId());

        if (!AppKeyAccess.isAllowed(app.getStatus(), appInstance.getStatus())) {
            return false;
        }

        Optional<User> userOptional = userService.fetchUserByLogin(principal.getName());

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        if (!user.isActivated()) {
            return false;
        }

        Long appInstanceId = appInstance.getId();

        return permissionService.hasResourceScope(appInstanceId, "AppInstance", "APP_USE");
    }

    private Optional<AppInstance> fetchAppInstance(String clientId) {
        Optional<String> uuidOptional = AppInstanceClients.parseInstanceUuid(clientId);

        if (uuidOptional.isEmpty()) {
            return Optional.empty();
        }

        return appInstanceService.fetchAppInstanceByUuid(uuidOptional.get());
    }
}
