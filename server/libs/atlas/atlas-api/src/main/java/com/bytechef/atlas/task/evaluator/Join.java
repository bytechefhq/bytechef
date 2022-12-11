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

package com.bytechef.atlas.task.evaluator;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
class Join implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
        String separator = (String) aArguments[0];
        List<?> values = (List<?>) aArguments[1];
        String str = values.stream().map(String::valueOf).collect(Collectors.joining(separator));

        return new TypedValue(str);
    }
}
