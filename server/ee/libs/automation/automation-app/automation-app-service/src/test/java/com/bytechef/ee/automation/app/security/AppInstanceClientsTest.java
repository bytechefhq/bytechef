/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppInstanceClientsTest {

    @Mock
    private ObjectProvider<OAuth2AuthorizationGrantService> authorizationGrantServiceProvider;

    @Mock
    private RegisteredClientService registeredClientService;

    @Mock
    private ObjectProvider<RegisteredClientService> registeredClientServiceProvider;

    private AppInstance appInstance;
    private AppInstanceClients appInstanceClients;

    @BeforeEach
    void beforeEach() {
        lenient().when(registeredClientServiceProvider.getIfAvailable())
            .thenReturn(registeredClientService);
        lenient().when(registeredClientServiceProvider.getObject())
            .thenReturn(registeredClientService);

        appInstance = new AppInstance();

        appInstance.setEnvironment(Environment.DEVELOPMENT);
        appInstance.setUrl("http://crm.internal:3000");
        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);
        appInstance.setUuid("instance-uuid");

        appInstanceClients = new AppInstanceClients(authorizationGrantServiceProvider, registeredClientServiceProvider);
    }

    @Test
    void testARedirectUriTheServerRefusesOnRegisterIsAnInvalidInstanceUrl() {
        doThrow(invalidRedirectUri()).when(registeredClientService)
            .registerClient(any());

        assertThatThrownBy(() -> appInstanceClients.registerClient(appInstance, "crm"))
            .isInstanceOfSatisfying(ConfigurationException.class, exception -> {
                assertThat(exception.getErrorKey()).isEqualTo(AppErrorType.APP_INSTANCE_URL_INVALID.getErrorKey());
                assertThat(exception.getMessage()).doesNotContain("crm.internal");
            });
    }

    @Test
    void testARedirectUriTheServerRefusesOnUpdateIsAnInvalidInstanceUrl() {
        doThrow(invalidRedirectUri()).when(registeredClientService)
            .updateClient(any());

        assertThatThrownBy(() -> appInstanceClients.updateClient(appInstance, "crm"))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_INSTANCE_URL_INVALID.getErrorKey()));
    }

    @Test
    void testAnotherRegistrationErrorIsKept() {
        ConfigurationException otherException = new ConfigurationException(
            "taken", RegisteredClientErrorType.REGISTERED_CLIENT_MANAGED_BY_SERVER);

        doThrow(otherException).when(registeredClientService)
            .registerClient(any());

        assertThatThrownBy(() -> appInstanceClients.registerClient(appInstance, "crm"))
            .isSameAs(otherException);
    }

    private static ConfigurationException invalidRedirectUri() {
        return new ConfigurationException(
            "OAuth2 redirect URI http://crm.internal:3000/auth/callback must be https",
            RegisteredClientErrorType.INVALID_REDIRECT_URI);
    }
}
