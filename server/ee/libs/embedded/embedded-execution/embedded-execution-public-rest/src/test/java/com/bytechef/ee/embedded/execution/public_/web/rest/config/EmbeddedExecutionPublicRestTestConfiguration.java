/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest.config;

import com.bytechef.ee.embedded.execution.public_.web.rest.ToolInvocationApiController;
import com.bytechef.jackson.config.JacksonConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@Import({
    JacksonConfiguration.class, ToolInvocationApiController.class
})
public class EmbeddedExecutionPublicRestTestConfiguration {
}
