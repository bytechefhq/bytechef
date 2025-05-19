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

package com.bytechef.evaluator;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Ivica Cardic
 */
class Plus implements MethodExecutor {

    private final TemporalUnit temporalUnit;

    Plus(TemporalUnit temporalUnit) {
        this.temporalUnit = temporalUnit;
    }

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        if (arguments[0] instanceof Temporal temporal) {
            return new TypedValue(temporal.plus((Integer) arguments[1], temporalUnit));
        } else {
            throw new IllegalArgumentException("Invalid arguments for plusDays.");
        }
    }
}
