/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.web.rest.mapper;

import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.ee.embedded.workflow.execution.web.rest.mapper.config.EmbeddedWorkflowExecutionMapperSpringConfig;
import com.bytechef.ee.embedded.workflow.execution.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.ee.embedded.workflow.execution.web.rest.model.WorkflowExecutionModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkflowExecutionMapper {

    @Mapper(config = EmbeddedWorkflowExecutionMapperSpringConfig.class, implementationName = "Embedded<CLASS_NAME>Impl")
    public interface WorkflowExecutionDTOToWorkflowExecutionModelMapper
        extends Converter<WorkflowExecutionDTO, WorkflowExecutionModel> {

        @Override
        WorkflowExecutionModel convert(WorkflowExecutionDTO workflowExecution);
    }

    @Mapper(config = EmbeddedWorkflowExecutionMapperSpringConfig.class, implementationName = "Embedded<CLASS_NAME>Impl")
    public interface WorkflowExecutionDTOToWorkflowExecutionBasicModelMapper
        extends Converter<WorkflowExecutionDTO, WorkflowExecutionBasicModel> {

        @Override
        WorkflowExecutionBasicModel convert(WorkflowExecutionDTO workflowExecution);
    }
}
