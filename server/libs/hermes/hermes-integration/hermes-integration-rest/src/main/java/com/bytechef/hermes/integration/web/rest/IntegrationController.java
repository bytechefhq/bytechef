/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.integration.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.facade.IntegrationFacade;
import com.bytechef.hermes.integration.service.IntegrationService;
import com.bytechef.hermes.integration.web.rest.model.IntegrationModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
public class IntegrationController implements IntegrationsApi {

    private final ConversionService conversionService;
    private final IntegrationFacade integrationFacade;
    private final IntegrationService integrationService;

    @SuppressFBWarnings("EI2")
    public IntegrationController(
            ConversionService conversionService,
            IntegrationFacade integrationFacade,
            IntegrationService integrationService) {
        this.conversionService = conversionService;
        this.integrationFacade = integrationFacade;
        this.integrationService = integrationService;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteIntegration(String id, ServerWebExchange exchange) {
        integrationService.delete(id);

        return Mono.just(ResponseEntity.ok().build());
    }

    @Override
    public Mono<ResponseEntity<IntegrationModel>> getIntegration(String id, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(
                conversionService.convert(integrationService.getIntegration(id), IntegrationModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<IntegrationModel>>> getIntegrations(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(integrationService.getIntegrations().stream()
                .map(integration -> conversionService.convert(integration, IntegrationModel.class))
                .toList())));
    }

    @Override
    @Transactional
    public Mono<ResponseEntity<IntegrationModel>> postIntegration(
            Mono<IntegrationModel> integrationModelMono, ServerWebExchange exchange) {

        return integrationModelMono.map(integrationModel -> ResponseEntity.ok(conversionService.convert(
                integrationFacade.add(conversionService.convert(integrationModel, Integration.class)),
                IntegrationModel.class)));
    }

    @Override
    public Mono<ResponseEntity<IntegrationModel>> putIntegration(
            String id, Mono<IntegrationModel> integrationModelMono, ServerWebExchange exchange) {
        return integrationModelMono.map(integrationModel -> {
            Integration integration = conversionService.convert(integrationModel, Integration.class);

            integration.setId(id);

            return ResponseEntity.ok(
                    conversionService.convert(integrationService.update(integration), IntegrationModel.class));
        });
    }
}
