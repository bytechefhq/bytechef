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
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class IconController {

    static final CacheControl CACHE_CONTROL = CacheControl.maxAge(1, TimeUnit.HOURS)
        .noTransform()
        .mustRevalidate();

    private final ComponentDefinitionService componentDefinitionService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    public IconController(
        ComponentDefinitionService componentDefinitionService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @GetMapping(value = "/icons/{name}.svg", produces = "image/svg+xml")
    public ResponseEntity<String> getIcon(@PathVariable("name") String name) {
        String icon = componentDefinitionService
            .fetchComponentDefinition(name, null)
            .map(ComponentDefinition::getIcon)
            .or(() -> taskDispatcherDefinitionService.fetchTaskDispatcherDefinition(name, null)
                .map(TaskDispatcherDefinition::getIcon))
            .orElse(null);

        if (icon == null) {
            return ResponseEntity.noContent()
                .build();
        }

        return ResponseEntity.ok()
            .cacheControl(CACHE_CONTROL)
            .eTag(computeETag(icon))
            .body(icon);
    }

    private static String computeETag(String icon) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(icon.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of()
                .formatHex(digest);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }
}
