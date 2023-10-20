
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

package com.bytechef.hermes.integration.web.rest.mapper;

import com.bytechef.hermes.integration.web.rest.mapper.config.IntegrationMapperSpringConfig;
import com.bytechef.hermes.integration.web.rest.model.TagModel;
import com.bytechef.tag.domain.Tag;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;

import java.util.Random;

/**
 * @author Ivica Cardic
 */
@Mapper(config = IntegrationMapperSpringConfig.class)
public abstract class TagModelMapper implements Converter<TagModel, Tag> {

    private static final Random RANDOM = new Random();

    public abstract Tag convert(TagModel tagModel);

    @AfterMapping
    public void afterMapping(@MappingTarget Tag tag) {
        if (tag.getId() == null) {
            tag.setId(-Math.abs(RANDOM.nextLong(Long.MAX_VALUE)));
        }
    }
}
