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
import java.time.ZoneId;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Ivica Cardic
 */
class AtZone implements MethodExecutor {

    private static final ConversionService conversionService = DefaultConversionService.getSharedInstance();

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        Instant instant = conversionService.convert(arguments[0], Instant.class);

        if (instant == null) {
            throw new IllegalArgumentException("Invalid arguments for atZone.");
        }

        ZoneId zoneId = conversionService.convert(arguments[1], ZoneId.class);

        if (zoneId == null) {
            throw new IllegalArgumentException("Invalid arguments for atZone.");
        }

        return new TypedValue(instant.atZone(zoneId));
    }
}
