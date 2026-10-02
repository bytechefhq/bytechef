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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentFacadeRubyUploadTest {

    @Test
    void testSaveRejectsRubyUpload() {
        CustomComponentService customComponentService = mock(CustomComponentService.class);
        CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);

        CustomComponentFacadeImpl customComponentFacade = new CustomComponentFacadeImpl(
            new ApplicationProperties(), mock(CacheManager.class), customComponentService, customComponentFileStorage);

        assertThatThrownBy(() -> customComponentFacade.save(new byte[0], Language.RUBY))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(thrown -> {
                ConfigurationException configurationException = (ConfigurationException) thrown;

                assertThat(configurationException.getErrorKey()).isEqualTo(103);
                assertThat(configurationException.getEntityClass()).isEqualTo(CustomComponent.class);
            });

        verifyNoInteractions(customComponentService, customComponentFileStorage);
    }
}
