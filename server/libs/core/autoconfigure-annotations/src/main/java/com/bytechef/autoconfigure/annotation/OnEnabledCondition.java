
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.autoconfigure.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class OnEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(
        ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {

        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(
            annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnEnabled.class.getName()));

        Environment environment = conditionContext.getEnvironment();
        String[] properties =
            annotationAttributes == null ? new String[0] : annotationAttributes.getStringArray("value");
        List<Boolean> results = new ArrayList<>();

        for (String property : properties) {
            String value = environment.getProperty("bytechef." + property + ".enabled");

            if (value != null) {
                results.add(Boolean.parseBoolean(value));
            }
        }

        Stream<Boolean> stream = results.stream();

        boolean result = results.isEmpty() || stream.allMatch(curResult -> curResult);

        return new ConditionOutcome(
            result,
            ConditionMessage
                .forCondition(ConditionalOnEnabled.class, "(" + getClass().getName() + ")")
                .resultedIn(result));
    }
}
