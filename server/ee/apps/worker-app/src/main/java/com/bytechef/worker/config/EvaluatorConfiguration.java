/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.worker.config;

import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class EvaluatorConfiguration {

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.create();
    }
}
