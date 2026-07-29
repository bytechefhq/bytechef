/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

/**
 * Spring AI {@link org.springframework.ai.chat.model.ToolContext} keys read by
 * {@link IntegrationWorkflowExecutionTools} to scope environment-bounded queries.
 *
 * <p>
 * The literal value intentionally matches the key written by the EE AI Hub tool invocation context so the runtime needs
 * no change to populate it. Unlike the automation analog there is no {@code WORKSPACE_ID} key: embedded deployments are
 * tenant-scoped (the ambient tenant is the isolation boundary) and have no workspace subdivision to guard against.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class IntegrationWorkflowExecutionToolContextKeys {

    public static final String ENVIRONMENT_ID = "bytechef.assetFile.environmentId";

    private IntegrationWorkflowExecutionToolContextKeys() {
    }
}
