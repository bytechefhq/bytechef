/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

/**
 * Verifies that {@link CustomComponentFacadeImpl#save} rejects Java (jar) uploads when they are disabled via
 * {@code bytechef.component.custom-component.java-enabled=false}, short-circuiting before any persistence, while
 * uploads in other languages are not gated by the flag.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentFacadeUploadDisabledTest {

    @Test
    void testSaveRejectsJavaUploadWhenJavaDisabled() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);
        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(false), mock(CacheManager.class), customComponentService, customComponentFileStorage);

        assertThatThrownBy(() -> customComponentFacade.save(new byte[0], Language.JAVA))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(thrown -> {
                ConfigurationException configurationException = (ConfigurationException) thrown;

                assertThat(configurationException.getErrorKey()).isEqualTo(100);
                assertThat(configurationException.getEntityClass()).isEqualTo(CustomComponent.class);
            });

        verifyNoInteractions(customComponentService, customComponentFileStorage);
    }

    @Test
    void testSaveDoesNotGateNonJavaUploadWhenJavaDisabled() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);
        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            applicationProperties(false), mock(CacheManager.class), customComponentService, customComponentFileStorage);

        // A JavaScript upload gets past the Java-only disable guard; it may still fail downstream on the empty payload,
        // but never with the disabled ConfigurationException.
        assertThatThrownBy(() -> customComponentFacade.save(new byte[0], Language.JAVASCRIPT))
            .isNotInstanceOf(ConfigurationException.class);
    }

    private static ApplicationProperties applicationProperties(boolean javaEnabled) {
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.getCustomComponent()
            .setJavaEnabled(javaEnabled);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getComponent()).thenReturn(component);

        return applicationProperties;
    }
}
