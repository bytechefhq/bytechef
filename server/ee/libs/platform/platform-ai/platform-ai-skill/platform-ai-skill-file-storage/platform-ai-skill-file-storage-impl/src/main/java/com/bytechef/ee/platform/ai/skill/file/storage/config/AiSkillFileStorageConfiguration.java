/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.file.storage.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.ee.platform.ai.skill.file.storage.AiSkillFileStorage;
import com.bytechef.ee.platform.ai.skill.file.storage.AiSkillFileStorageImpl;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
class AiSkillFileStorageConfiguration {

    @Bean
    AiSkillFileStorage aiSkillFileStorage(
        ApplicationProperties applicationProperties, FileStorageServiceRegistry fileStorageServiceRegistry) {

        Provider provider = applicationProperties.getFileStorage()
            .getProvider();

        return new AiSkillFileStorageImpl(fileStorageServiceRegistry.getFileStorageService(provider.name()));
    }
}
