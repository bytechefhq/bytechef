/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.aws;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.aws.connection.AwsConnection;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * The connection an AWS Batch task runner uses to reach an AWS account. It has no actions of its own.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component("aws_v1_ComponentHandler")
@ConditionalOnEEVersion
public class AwsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("aws")
        .title("AWS")
        .description("Runs script and commands tasks as AWS Batch jobs in an AWS account.")
        .icon("path:assets/aws.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(AwsConnection.CONNECTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
