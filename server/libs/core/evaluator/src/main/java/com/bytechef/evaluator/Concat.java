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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.evaluator;

import java.util.ArrayList;
import java.util.List;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
class Concat implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext context, Object aTarget, Object... arguments) throws AccessException {
        List<?> l1 = (List<?>) arguments[0];
        List<?> l2 = (List<?>) arguments[1];

        List<Object> joined = new ArrayList<>(l1.size() + l2.size());

        joined.addAll(l1);
        joined.addAll(l2);

        return new TypedValue(joined);
    }
}
