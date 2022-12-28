
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

import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.domain.IntegrationWorkflow;
import com.bytechef.hermes.integration.web.rest.mapper.config.IntegrationMapperSpringConfig;
import com.bytechef.hermes.integration.web.rest.model.IntegrationModel;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = IntegrationMapperSpringConfig.class)
public abstract class IntegrationMapper implements Converter<Integration, IntegrationModel> {

    @Autowired
    private TagService tagService;

    @Mapping(target = "tags", ignore = true)
    public abstract IntegrationModel convert(Integration integration);

    public String map(IntegrationWorkflow integrationWorkflow) {
        return integrationWorkflow.getWorkflowId();
    }

    @AfterMapping
    protected void afterMapping(Integration integration, @MappingTarget IntegrationModel integrationModel) {
        integrationModel.setTags(tagService.getTags(integration.getTagIds())
            .stream()
            .map(Tag::getName)
            .toList());
    }
}
