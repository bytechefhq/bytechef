/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.config.PlatformSecurityIntTestConfiguration;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformSecurityIntTestConfiguration.class)
class ApiKeyServiceIntTest {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void beforeEach() {
        user = userRepository.save(newUser());
    }

    @AfterEach
    void afterEach() {
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreateGeneratesSecretKey() {
        ApiKey createdApiKey = apiKeyService.create(newApiKey("automation key"));

        assertThat(createdApiKey.getSecretKey()).isNotBlank();
        assertThat(apiKeyRepository.findById(createdApiKey.getId()))
            .hasValueSatisfying(apiKey -> {
                assertThat(apiKey.getName()).isEqualTo("automation key");
                assertThat(apiKey.getSecretKey()).isEqualTo(createdApiKey.getSecretKey());
            });
        assertThat(apiKeyService.getApiKey(createdApiKey.getSecretKey())
            .getId()).isEqualTo(createdApiKey.getId());
    }

    @Test
    void testDelete() {
        ApiKey deletedApiKey = apiKeyService.create(newApiKey("deleted key"));
        ApiKey keptApiKey = apiKeyService.create(newApiKey("kept key"));

        apiKeyService.delete(deletedApiKey.getId());

        assertThat(apiKeyRepository.findById(deletedApiKey.getId())).isEmpty();
        assertThat(apiKeyRepository.findById(keptApiKey.getId())).isPresent();
    }

    private ApiKey newApiKey(String name) {
        ApiKey apiKey = new ApiKey();

        apiKey.setName(name);
        apiKey.setType(PlatformType.AUTOMATION);
        apiKey.setUserId(user.getId());

        return apiKey;
    }

    private static User newUser() {
        User user = new User();

        user.setActivated(true);
        user.setEmail("api-key-service@localhost");
        user.setLangKey("en");
        user.setLogin("api-key-service");
        user.setPassword("$2a$10$" + "a".repeat(53));
        user.setUuid(UUID.randomUUID());

        return user;
    }
}
