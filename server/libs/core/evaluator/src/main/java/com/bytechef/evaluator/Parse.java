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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Ivica Cardic
 */
class Parse implements MethodExecutor {

    Parse(Type type) {
        this.type = type;
    }

    enum Type {
        DATE,
        DATE_TIME
    }

    private final Type type;

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        if (type == Type.DATE) {
            if (arguments.length == 2) {
                return new TypedValue(
                    ZonedDateTime.parse((String) arguments[0], DateTimeFormatter.ofPattern((String) arguments[1])));
            } else {
                return new TypedValue(LocalDate.parse((String) arguments[0]));
            }
        } else {
            if (arguments.length == 2) {
                return new TypedValue(
                    LocalDateTime.parse((String) arguments[0], DateTimeFormatter.ofPattern((String) arguments[1])));
            } else {
                return new TypedValue(LocalDateTime.parse((String) arguments[0]));
            }
        }
    }
}
