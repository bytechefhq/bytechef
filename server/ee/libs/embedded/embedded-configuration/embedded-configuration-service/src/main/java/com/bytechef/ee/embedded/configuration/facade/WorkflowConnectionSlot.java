/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkflowConnectionSlot {

    private final ComponentConnectionFacade componentConnectionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowConnectionSlot(ComponentConnectionFacade componentConnectionFacade,
        WorkflowService workflowService) {

        this.componentConnectionFacade = componentConnectionFacade;
        this.workflowService = workflowService;
    }

    public List<ComponentConnection> getSlots(String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return CollectionUtils.concat(
            WorkflowTrigger.of(workflow)
                .stream()
                .flatMap(workflowTrigger -> CollectionUtils.stream(
                    componentConnectionFacade.getComponentConnections(workflowTrigger)))
                .toList(),
            workflow.getTasks(true)
                .stream()
                .flatMap(workflowTask -> CollectionUtils.stream(
                    componentConnectionFacade.getComponentConnections(workflowTask)))
                .toList());
    }
}
