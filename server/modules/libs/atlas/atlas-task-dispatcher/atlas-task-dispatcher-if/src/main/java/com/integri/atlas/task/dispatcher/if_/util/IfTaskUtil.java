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

package com.integri.atlas.task.dispatcher.if_.util;

import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Matija Petanjek
 */
public class IfTaskUtil {

    public static boolean resolveCase(TaskEvaluator taskEvaluator, TaskExecution aIfTask) {
        List<MapObject> conditions = aIfTask.getList("conditions", MapObject.class);
        String combineOperation = aIfTask.getRequiredString("combineOperation");

        return taskEvaluator.evaluate(
            String.join(getBooleanOperator(combineOperation), getConditions(conditions)),
            Boolean.class
        );
    }

    private static List<String> getConditions(List<MapObject> aConditions) {
        List<String> conditions = new ArrayList<>();

        for (MapObject condition : aConditions) {
            for (String key : condition.keySet()) {
                MapObject conditionParts = condition.get(key, MapObject.class);

                conditions.add(
                    conditionParts.getRequiredString("value1") +
                    conditionParts.getRequiredString("operation") +
                    conditionParts.getRequiredString("value2")
                );
            }
        }

        return conditions;
    }

    private static String getBooleanOperator(String combineOperation) {
        if (combineOperation.equals("any")) {
            return "||";
        } else if (combineOperation.equals("all")) {
            return "&&";
        }

        throw new IllegalArgumentException("Invalid combine operation: " + combineOperation);
    }
}
