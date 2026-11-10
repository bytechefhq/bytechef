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

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Ivica Cardic
 */
class Exists implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        if (arguments.length != 1 || !(arguments[0] instanceof DataPillPath dataPillPath)) {
            throw new AccessException("exists takes one argument, a data pill, e.g. exists(${x})");
        }

        return new TypedValue(dataPillPath.resolve(target) != null);
    }
}
