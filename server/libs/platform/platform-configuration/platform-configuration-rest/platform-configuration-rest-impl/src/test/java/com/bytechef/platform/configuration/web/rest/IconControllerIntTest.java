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

package com.bytechef.platform.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.web.rest.config.PlatformConfigurationRestTestConfiguration;
import com.bytechef.platform.configuration.web.rest.config.WorkflowConfigurationRestTestConfigurationSharedMocks;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = PlatformConfigurationRestTestConfiguration.class)
@WebMvcTest(IconController.class)
@WorkflowConfigurationRestTestConfigurationSharedMocks
public class IconControllerIntTest {

    private static final String ICON = "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>";

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @MockitoBean
    private JobPrincipalAccessor jobPrincipalAccessor;

    @MockitoBean
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    public void beforeEach() {
        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(any())).thenReturn(jobPrincipalAccessor);

        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    public void testGetIconReturnsIconWithETagAndRevalidatingCacheControl() {
        mockComponentIcon(ICON);

        webTestClient
            .get()
            .uri("/icons/ftp.svg")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .exists(HttpHeaders.ETAG)
            .expectHeader()
            .cacheControl(IconController.CACHE_CONTROL)
            .expectBody(String.class)
            .isEqualTo(ICON);
    }

    @Test
    public void testGetIconReturnsNotModifiedWhenETagMatches() {
        mockComponentIcon(ICON);

        String eTag = getETag();

        webTestClient
            .get()
            .uri("/icons/ftp.svg")
            .header(HttpHeaders.IF_NONE_MATCH, eTag)
            .exchange()
            .expectStatus()
            .isNotModified();
    }

    @Test
    public void testGetIconReturnsChangedIconWhenCachedETagIsStale() {
        mockComponentIcon(ICON);

        String staleETag = getETag();

        String changedIcon = "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle/></svg>";

        mockComponentIcon(changedIcon);

        String newETag = webTestClient
            .get()
            .uri("/icons/ftp.svg")
            .header(HttpHeaders.IF_NONE_MATCH, staleETag)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .isEqualTo(changedIcon)
            .returnResult()
            .getResponseHeaders()
            .getETag();

        assertThat(newETag).isNotEqualTo(staleETag);
    }

    private String getETag() {
        return webTestClient
            .get()
            .uri("/icons/ftp.svg")
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(String.class)
            .getResponseHeaders()
            .getETag();
    }

    private void mockComponentIcon(String icon) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getIcon()).thenReturn(icon);

        when(componentDefinitionService.fetchComponentDefinition("ftp", null))
            .thenReturn(Optional.of(componentDefinition));
    }
}
