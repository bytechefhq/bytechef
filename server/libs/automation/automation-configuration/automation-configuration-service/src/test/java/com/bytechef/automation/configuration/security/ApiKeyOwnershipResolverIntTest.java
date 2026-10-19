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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.WorkspaceApiKeyIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@WorkspaceApiKeyIntTestConfigurationSharedMocks
class ApiKeyOwnershipResolverIntTest {

    private static final long OWNER_USER_ID = 10L;

    @Autowired
    private ApiKeyOwnershipResolver apiKeyOwnershipResolver;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private List<ResourceOwnershipResolver> resourceOwnershipResolvers;

    @Autowired
    private WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("api-key-ownership"));

        workspaceId = workspace.getId();
    }

    @AfterEach
    void afterEach() {
        workspaceApiKeyRepository.deleteAll();
        apiKeyRepository.deleteAll();
        workspaceRepository.deleteById(workspaceId);
    }

    @Test
    void testResourceType() {
        assertThat(apiKeyOwnershipResolver.resourceType()).isEqualTo("ApiKey");
        assertThat(resourceOwnershipResolvers)
            .filteredOn(resourceOwnershipResolver -> "ApiKey".equals(resourceOwnershipResolver.resourceType()))
            .containsExactly(apiKeyOwnershipResolver);
    }

    @Test
    void testResolvesTheWorkspaceAndTheOwnerOfAWorkspaceKey() {
        ApiKey apiKey = createApiKey();

        workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKey.getId(), workspaceId));

        ResourceOwner resourceOwner = apiKeyOwnershipResolver.resolveOwner(apiKey.getId());

        assertThat(resourceOwner.workspaceId()).hasValue(workspaceId);
        assertThat(resourceOwner.ownerUserId()).hasValue(OWNER_USER_ID);
    }

    @Test
    void testResolvesOnlyTheOwnerOfAKeyBoundToNoWorkspace() {
        ApiKey apiKey = createApiKey();

        ResourceOwner resourceOwner = apiKeyOwnershipResolver.resolveOwner(apiKey.getId());

        assertThat(resourceOwner.workspaceId()).isEmpty();
        assertThat(resourceOwner.ownerUserId()).hasValue(OWNER_USER_ID);
    }

    @Test
    void testAnUnknownKeyIsUnknown() {
        ResourceOwner resourceOwner = apiKeyOwnershipResolver.resolveOwner(Long.MAX_VALUE);

        assertThat(resourceOwner.workspaceId()).isEmpty();
        assertThat(resourceOwner.ownerUserId()).isEmpty();
    }

    private ApiKey createApiKey() {
        ApiKey apiKey = new ApiKey();

        apiKey.setName("ownership key");
        apiKey.setType(PlatformType.AUTOMATION);
        apiKey.setUserId(OWNER_USER_ID);

        return apiKeyService.create(apiKey);
    }
}
