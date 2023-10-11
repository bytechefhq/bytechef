
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atllas.worker.remote.config;

import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkerHandlerConfiguration {

    @Bean
    TaskHandlerRegistry taskHandlerRegistry(ActionDefinitionFacade actionDefinitionFacade) {
        return type -> (TaskHandler<?>) taskExecution -> {
            ComponentOperation componentOperation = ComponentOperation.ofType(type);

            Map<String, Long> connectIdMap = MapUtils.getMap(
                taskExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());

            return actionDefinitionFacade.executePerform(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName(), Validate.notNull(taskExecution.getId(), "id"),
                taskExecution.getParameters(),
                OptionalUtils.orElse(CollectionUtils.findFirst(connectIdMap.values()), null));
        };
    }
}
