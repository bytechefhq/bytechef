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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Ivica Cardic
 */
class Format implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        if (arguments[0] instanceof LocalDate localDate) {
            if (arguments.length == 2) {
                return new TypedValue(localDate.format(DateTimeFormatter.ofPattern((String) arguments[1])));
            } else {
                return new TypedValue(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        } else if (arguments[0] instanceof LocalDateTime localDateTime) {
            if (arguments.length == 2) {
                return new TypedValue(localDateTime.format(DateTimeFormatter.ofPattern((String) arguments[1])));
            } else {
                return new TypedValue(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        } else if (arguments[0] instanceof Instant instant) {
            DateTimeFormatter dateTimeFormatter;

            if (arguments.length == 2) {
                dateTimeFormatter = DateTimeFormatter.ofPattern((String) arguments[1]);
            } else {
                dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            }

            dateTimeFormatter = dateTimeFormatter.withZone(ZoneId.systemDefault());

            return new TypedValue(dateTimeFormatter.format(instant));
        } else {
            Object[] args = new Object[arguments.length - 1];

            System.arraycopy(arguments, 1, args, 0, arguments.length - 1);

            return new TypedValue(String.format((String) arguments[0], args));
        }
    }
}
