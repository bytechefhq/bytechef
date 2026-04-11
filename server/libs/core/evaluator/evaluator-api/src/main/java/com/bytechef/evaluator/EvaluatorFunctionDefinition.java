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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * Describes an evaluator function available in the workflow expression language.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record EvaluatorFunctionDefinition(
    String name,
    String title,
    String description,
    EvaluatorFunctionCategory category,
    List<EvaluatorFunctionParameter> parameters,
    EvaluatorFunctionType returnType,
    String example) {

    /**
     * Describes a parameter of an evaluator function.
     *
     * @author Ivica Cardic
     */
    public record EvaluatorFunctionParameter(
        String name, String description, EvaluatorFunctionType type, boolean required) {
    }
}
