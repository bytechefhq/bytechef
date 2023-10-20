/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.engine.core.task.evaluator.spel;

import com.integri.atlas.engine.core.json.JSONHelper;
import java.util.List;
import java.util.Map;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Ivica Cardic
 */
public class ParseJSON implements MethodExecutor {

    private final JSONHelper jsonHelper;

    public ParseJSON(JSONHelper jsonHelper) {
        this.jsonHelper = jsonHelper;
    }

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        String json = (String) arguments[0];

        try {
            return new TypedValue(jsonHelper.deserialize(json, Map.class));
        } catch (RuntimeException e) {
            return new TypedValue(jsonHelper.deserialize(json, List.class));
        }
    }
}
