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
import com.bytechef.commons.util.ObfuscateUtils;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import com.bytechef.platform.configuration.web.rest.model.AiProviderModel;
import com.bytechef.platform.configuration.web.rest.model.UpdateAiProviderRequestModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class AiProviderApiController implements AiProviderApi {

    private final AiProviderFacade aiProviderFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public AiProviderApiController(AiProviderFacade aiProviderFacade, ConversionService conversionService) {
        this.aiProviderFacade = aiProviderFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> deleteAiProvider(Integer id, Integer environment) {
        aiProviderFacade.deleteAiProvider(id, environment);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<List<AiProviderModel>> getAiProviders(Integer environment) {
        return ResponseEntity.ok(
            aiProviderFacade.getAiProviders(environment)
                .stream()
                .map(aiProviderDTO -> conversionService.convert(aiProviderDTO, AiProviderModel.class))
                .peek(aiProviderModel -> {
                    if (aiProviderModel != null) {
                        aiProviderModel.setApiKey(ObfuscateUtils.obfuscate(aiProviderModel.getApiKey(), 26, 6));
                    }
                })
                .toList());
    }

    @Override
    public ResponseEntity<Void> enableAiProvider(Integer id, Boolean enable, Integer environment) {
        aiProviderFacade.updateAiProvider(id, enable, environment);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateAiProvider(
        Integer id, Integer environment, UpdateAiProviderRequestModel updateAiProviderRequestModel) {

        aiProviderFacade.updateAiProvider(
            id, updateAiProviderRequestModel.getApiKey(), updateAiProviderRequestModel.getUrl(), environment);

        return ResponseEntity.noContent()
            .build();
    }
}
