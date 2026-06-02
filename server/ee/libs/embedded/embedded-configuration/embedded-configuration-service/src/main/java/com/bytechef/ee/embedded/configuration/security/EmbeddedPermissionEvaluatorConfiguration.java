/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration
@ConditionalOnBean(Evaluator.class)
@ConditionalOnEEVersion
public class EmbeddedPermissionEvaluatorConfiguration {

    @Bean
    EmbeddedPermissionEvaluator embeddedPermissionEvaluator(Evaluator evaluator) {
        return new EmbeddedPermissionEvaluator(evaluator);
    }
}
