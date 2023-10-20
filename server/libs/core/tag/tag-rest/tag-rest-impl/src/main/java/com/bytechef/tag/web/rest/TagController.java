
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

package com.bytechef.tag.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
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
public class TagController implements TagsApi {

    private final TagService tagService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI2")
    public TagController(TagService tagService, ConversionService conversionService) {
        this.tagService = tagService;
        this.conversionService = conversionService;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteTag(Long id, ServerWebExchange exchange) {
        tagService.delete(id);

        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity<TagModel>> getTag(Long id, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(
            conversionService.convert(tagService.getTag(id), TagModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<TagModel>>> getTags(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(tagService.getTags()
            .stream()
            .map(tag -> conversionService.convert(tag, TagModel.class))
            .toList())));
    }

    @Override
    public Mono<ResponseEntity<TagModel>> postTag(Mono<TagModel> tagModelMono, ServerWebExchange exchange) {
        return tagModelMono.map(tagModel -> ResponseEntity.ok(
            conversionService.convert(
                tagService.create(conversionService.convert(tagModel, Tag.class)),
                TagModel.class)));
    }

    @Override
    public Mono<ResponseEntity<TagModel>> putTag(Long id, Mono<TagModel> tagModelMono, ServerWebExchange exchange) {
        return tagModelMono.map(tagModel -> ResponseEntity.ok(
            conversionService.convert(
                tagService.update(conversionService.convert(tagModel.id(id), Tag.class)),
                TagModel.class)));
    }
}
