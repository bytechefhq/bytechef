/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppContextDTO;
import com.bytechef.ee.automation.app.dto.AppCurrentUserDTO;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.model.AppContextModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.AppCurrentUserModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The context and signed-in user endpoints an app calls with its own secret key.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppContextApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppContextApiController extends AbstractAppApiController implements AppContextApi {

    private final AppExecutionFacade appExecutionFacade;

    @SuppressFBWarnings("EI2")
    public AppContextApiController(AppExecutionFacade appExecutionFacade) {
        this.appExecutionFacade = appExecutionFacade;
    }

    @Override
    public ResponseEntity<AppContextModel> getContext() {
        return ResponseEntity.ok(toModel(appExecutionFacade.getContext()));
    }

    @Override
    public ResponseEntity<AppCurrentUserModel> getCurrentUser() {
        return ResponseEntity.ok(toModel(appExecutionFacade.getCurrentUser()));
    }

    private static AppContextModel toModel(AppContextDTO appContext) {
        AppContextModel appContextModel = new AppContextModel(
            appContext.appId(), appContext.appName(), appContext.appInstanceId(), appContext.appInstanceName(),
            AppContextModel.EnvironmentEnum.fromValue(appContext.environment()
                .name()),
            new HashMap<>(appContext.configuration()),
            AppContextModel.UserAuthModeEnum.fromValue(appContext.userAuthMode()
                .name()));

        return appContextModel.issuer(appContext.issuer())
            .clientId(appContext.clientId())
            .authorizationEndpoint(appContext.authorizationEndpoint())
            .tokenEndpoint(appContext.tokenEndpoint());
    }

    private static AppCurrentUserModel toModel(AppCurrentUserDTO appCurrentUser) {
        AppCurrentUserModel appCurrentUserModel = new AppCurrentUserModel(
            appCurrentUser.id(), appCurrentUser.login(), appCurrentUser.scopes());

        return appCurrentUserModel.firstName(appCurrentUser.firstName())
            .lastName(appCurrentUser.lastName())
            .email(appCurrentUser.email())
            .workspaceRole(appCurrentUser.workspaceRole());
    }
}
