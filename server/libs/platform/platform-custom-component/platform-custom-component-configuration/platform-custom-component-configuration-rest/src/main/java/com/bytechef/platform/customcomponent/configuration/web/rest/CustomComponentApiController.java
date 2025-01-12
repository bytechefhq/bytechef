/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.customcomponent.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.custom.component.configuration.web.rest.CustomComponentApi;
import com.bytechef.platform.custom.component.configuration.web.rest.model.CustomComponentModel;
import com.bytechef.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.platform.customcomponent.configuration.service.CustomComponentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.platform.custom.component.configuration.web.rest.CustomComponentApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class CustomComponentApiController implements CustomComponentApi {

    private final CustomComponentFacade customComponentFacade;
    private final CustomComponentService customComponentService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public CustomComponentApiController(
        CustomComponentFacade customComponentFacade, CustomComponentService customComponentService,
        ConversionService conversionService) {

        this.customComponentFacade = customComponentFacade;
        this.customComponentService = customComponentService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> deleteCustomComponent(Long id) {
        customComponentFacade.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableCustomComponent(Long id, Boolean enable) {
        customComponentService.enableCustomComponent(id, enable);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<CustomComponentModel> getCustomComponent(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(customComponentService.getCustomComponent(id), CustomComponentModel.class));
    }

    @Override
    public ResponseEntity<List<CustomComponentModel>> getCustomComponents() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                customComponentFacade.getCustomComponents(),
                openSdkComponent -> conversionService.convert(openSdkComponent, CustomComponentModel.class)));
    }
}
