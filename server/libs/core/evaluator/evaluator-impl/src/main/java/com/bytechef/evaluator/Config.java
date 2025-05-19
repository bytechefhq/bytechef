/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.evaluator;

import org.springframework.core.env.Environment;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * @author Arik Cohen
 * @since Mar, 06 2020
 */
class Config implements MethodExecutor {

    private final transient Environment environment;

    public Config(Environment environment) {
        this.environment = environment;
    }

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        String propertyName = (String) arguments[0];
        String value = environment.getProperty(propertyName);

        if (value == null) {
            throw new SpelEvaluationException(
                SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, propertyName, Environment.class);
        }

        return new TypedValue(value);
    }
}
