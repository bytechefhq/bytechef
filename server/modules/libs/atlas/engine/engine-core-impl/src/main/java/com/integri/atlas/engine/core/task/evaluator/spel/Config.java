/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.core.task.evaluator.spel;

import org.springframework.core.env.Environment;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.stereotype.Component;

/**
 * @author Arik Cohen
 * @since Mar, 06 2020
 */
@Component
class Config implements MethodExecutor {

    private final Environment environment;

    public Config(Environment aEnvironment) {
        environment = aEnvironment;
    }

    @Override
    public TypedValue execute(EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
        String propertyName = (String) aArguments[0];
        String value = environment.getProperty(propertyName);
        if (value == null) {
            throw new SpelEvaluationException(
                SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE,
                propertyName,
                Environment.class
            );
        }
        return new TypedValue(value);
    }
}
