/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.tool;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * @author Ivica Cardic
 */
public class BraveWebSearchCondition extends SpringBootCondition {

    static final String API_KEY_PROPERTY = "bytechef.ai.brave.api-key";
    static final String ENABLED_PROPERTY = "bytechef.ai.brave.enabled";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();

        if (!environment.getProperty(ENABLED_PROPERTY, Boolean.class, false)) {
            return ConditionOutcome.noMatch(ENABLED_PROPERTY + " is not true");
        }

        if (!StringUtils.hasText(environment.getProperty(API_KEY_PROPERTY))) {
            return ConditionOutcome.noMatch(API_KEY_PROPERTY + " is not set");
        }

        return ConditionOutcome.match();
    }
}
