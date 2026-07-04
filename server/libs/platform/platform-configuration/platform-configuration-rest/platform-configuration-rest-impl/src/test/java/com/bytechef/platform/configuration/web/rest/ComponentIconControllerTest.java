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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/**
 * Unit test for {@link ComponentIconController}: inline SVG icons are served with cache and CORS headers, URL icons
 * redirect, and unknown components or missing icons yield 404.
 *
 * @author Ivica Cardic
 */
class ComponentIconControllerTest {

    private ComponentIconController componentIconController;
    private ComponentDefinitionService componentDefinitionService;

    @BeforeEach
    void setUp() {
        componentDefinitionService = mock(ComponentDefinitionService.class);

        componentIconController = new ComponentIconController(componentDefinitionService);
    }

    @Test
    void testGetComponentIconServesInlineSvg() {
        stubComponentIcon("googleMail", "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>");

        ResponseEntity<String> responseEntity = componentIconController.getComponentIcon("googleMail");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(responseEntity.getBody()).startsWith("<svg");

        HttpHeaders httpHeaders = responseEntity.getHeaders();

        assertThat(httpHeaders.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("image/svg+xml");
        assertThat(httpHeaders.getFirst(HttpHeaders.CACHE_CONTROL)).contains("max-age=86400");
        assertThat(httpHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
    }

    @Test
    void testGetComponentIconRedirectsUrlIcon() {
        stubComponentIcon("example", "https://static.example.com/icons/example.svg");

        ResponseEntity<String> responseEntity = componentIconController.getComponentIcon("example");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(302));
        assertThat(responseEntity.getHeaders()
            .getLocation()).isEqualTo(URI.create("https://static.example.com/icons/example.svg"));
    }

    @Test
    void testGetComponentIconReturnsNotFoundForMissingIcon() {
        stubComponentIcon("bare", null);

        ResponseEntity<String> responseEntity = componentIconController.getComponentIcon("bare");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(404));
    }

    @Test
    void testGetComponentIconReturnsNotFoundForUnknownComponent() {
        when(componentDefinitionService.getComponentDefinition("nope", null))
            .thenThrow(new IllegalArgumentException("Unknown component"));

        ResponseEntity<String> responseEntity = componentIconController.getComponentIcon("nope");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(404));
    }

    private void stubComponentIcon(String componentName, String icon) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getIcon()).thenReturn(icon);
        when(componentDefinitionService.getComponentDefinition(componentName, null)).thenReturn(componentDefinition);
    }
}
