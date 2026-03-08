/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.registry;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteSubflowResolverClient implements SubflowResolver {

    @Override
    public Subflow resolveSubflow(String workflowUuid, String triggerName, boolean editorEnvironment) {
        throw new UnsupportedOperationException();
    }
}
