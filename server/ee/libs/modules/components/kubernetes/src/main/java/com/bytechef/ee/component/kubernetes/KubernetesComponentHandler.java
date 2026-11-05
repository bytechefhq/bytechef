/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.kubernetes;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.kubernetes.connection.KubernetesConnection;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * The connection a Kubernetes task runner uses to reach a cluster. It has no actions of its own.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component("kubernetes_v1_ComponentHandler")
@ConditionalOnEEVersion
public class KubernetesComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("kubernetes")
        .title("Kubernetes")
        .description("Runs script and commands tasks as pods on a Kubernetes cluster.")
        .icon("path:assets/kubernetes.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(KubernetesConnection.CONNECTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
