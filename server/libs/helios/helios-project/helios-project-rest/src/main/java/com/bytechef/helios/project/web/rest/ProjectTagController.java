
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

package com.bytechef.helios.project.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.helios.project.facade.ProjectFacade;
import com.bytechef.tag.web.rest.model.TagModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
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
public class ProjectTagController implements ProjectTagsApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;

    @SuppressFBWarnings("EI")
    public ProjectTagController(ConversionService conversionService, ProjectFacade projectFacade) {
        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
    }

    @Override
    public Mono<ResponseEntity<Flux<TagModel>>> getProjectTags(ServerWebExchange exchange) {
        return Mono.just(
            Flux.fromIterable(
                projectFacade.getProjectTags()
                    .stream()
                    .map(tag -> conversionService.convert(tag, TagModel.class))
                    .toList()))
            .map(ResponseEntity::ok);
    }
}
