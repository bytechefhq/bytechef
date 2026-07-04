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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves component icons by raw component name. The endpoint is intentionally unauthenticated: icons are static product
 * assets (identical across tenants and deployments), and anonymous consumers — most notably the MCP App workflow viewer
 * widget running in a sandboxed iframe inside MCP hosts — have no ByteChef session to authenticate with.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class ComponentIconController {

    private static final MediaType IMAGE_SVG_XML = MediaType.valueOf("image/svg+xml");

    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings("EI")
    public ComponentIconController(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @GetMapping("/icons/components/{componentName}")
    public ResponseEntity<String> getComponentIcon(@PathVariable("componentName") String componentName) {
        ComponentDefinition componentDefinition;

        try {
            componentDefinition = componentDefinitionService.getComponentDefinition(componentName, null);
        } catch (Exception exception) {
            return ResponseEntity.notFound()
                .build();
        }

        String icon = componentDefinition.getIcon();

        if (icon == null || icon.isBlank()) {
            return ResponseEntity.notFound()
                .build();
        }

        icon = icon.strip();

        // Icons declared with the path: prefix are already inlined as SVG text by IconUtils.readIcon.
        if (icon.startsWith("<")) {
            return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .contentType(IMAGE_SVG_XML)
                .body(icon);
        }

        if (icon.startsWith("http://") || icon.startsWith("https://")) {
            return ResponseEntity.status(302)
                .location(URI.create(icon))
                .build();
        }

        return ResponseEntity.notFound()
            .build();
    }
}
