
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.web.rest.mapper;

import com.bytechef.athena.configuration.web.rest.mapper.config.IntegrationMapperSpringConfiguration;
import com.bytechef.athena.configuration.web.rest.model.WorkflowTaskModel;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.athena.configuration.web.rest.model.WorkflowModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Mapper(config = IntegrationMapperSpringConfiguration.class)
public interface IntegrationWorkflowMapper extends Converter<Workflow, WorkflowModel> {

    @Override
    WorkflowModel convert(Workflow workflow);

    WorkflowTaskModel map(WorkflowTask workflowTask);

    default Integer mapTointeger(Optional<Integer> optional) {
        return optional.orElse(null);
    }

    default String mapToString(Optional<String> optional) {
        return optional.orElse(null);
    }
}
