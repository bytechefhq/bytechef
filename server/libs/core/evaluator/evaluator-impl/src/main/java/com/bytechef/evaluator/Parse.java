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

import com.bytechef.commons.util.TemporalValueUtils;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.jspecify.annotations.Nullable;
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
        Object argument = TemporalValueUtils.normalize(arguments[0]);

        if (argument instanceof ZonedDateTime zonedDateTime) {
            return new TypedValue(type == Type.DATE ? zonedDateTime : zonedDateTime.toLocalDateTime());
        }

        if (argument instanceof LocalDateTime localDateTime) {
            return new TypedValue(type == Type.DATE ? localDateTime.atZone(ZoneOffset.UTC) : localDateTime);
        }

        if (argument instanceof LocalDate localDate) {
            return new TypedValue(type == Type.DATE ? localDate : localDate.atStartOfDay());
        }

        if (!(argument instanceof String text)) {
            throw new AccessException(
                "%s expects a string or a temporal value but received %s: %s".formatted(
                    functionName(), typeNameOf(argument), argument));
        }

        try {
            return new TypedValue(parseText(text, arguments));
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw new AccessException(
                "%s cannot parse %s: %s".formatted(functionName(), text, exception.getMessage()), exception);
        }
    }

    private Object parseText(String text, Object[] arguments) {
        if (type == Type.DATE) {
            if (arguments.length == 2) {
                return ZonedDateTime.parse(text, DateTimeFormatter.ofPattern((String) arguments[1]));
            }

            return LocalDate.parse(text);
        }

        if (arguments.length == 2) {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern((String) arguments[1]));
        }

        return LocalDateTime.parse(text);
    }

    private static String typeNameOf(@Nullable Object argument) {
        if (argument == null) {
            return "null";
        }

        Class<?> argumentClass = argument.getClass();

        return argumentClass.getName();
    }

    private String functionName() {
        return type == Type.DATE ? "parseDate" : "parseDateTime";
    }
}
