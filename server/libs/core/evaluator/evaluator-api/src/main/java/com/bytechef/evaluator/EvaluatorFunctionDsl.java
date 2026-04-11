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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fluent DSL for building {@link EvaluatorFunctionDefinition} instances.
 *
 * @author Ivica Cardic
 */
public final class EvaluatorFunctionDsl {

    private EvaluatorFunctionDsl() {
    }

    /**
     * Creates a new modifiable function definition with the given name.
     *
     * @param name the function name as it appears in expressions
     * @return a new {@link ModifiableEvaluatorFunctionDefinition}
     */
    public static ModifiableEvaluatorFunctionDefinition function(String name) {
        return new ModifiableEvaluatorFunctionDefinition(name);
    }

    /**
     * Creates a new modifiable parameter definition with the given name.
     *
     * @param name the parameter name
     * @return a new {@link ModifiableEvaluatorFunctionParameter}
     */
    public static ModifiableEvaluatorFunctionParameter parameter(String name) {
        return new ModifiableEvaluatorFunctionParameter(name);
    }

    /**
     * Mutable builder for {@link EvaluatorFunctionDefinition}.
     *
     * @author Ivica Cardic
     */
    public static final class ModifiableEvaluatorFunctionDefinition {

        private EvaluatorFunctionCategory category;
        private String description;
        private String example;
        private final String name;
        private List<ModifiableEvaluatorFunctionParameter> parameters = new ArrayList<>();
        private EvaluatorFunctionType returnType;
        private String title;

        private ModifiableEvaluatorFunctionDefinition(String name) {
            this.name = name;
        }

        public ModifiableEvaluatorFunctionDefinition title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableEvaluatorFunctionDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableEvaluatorFunctionDefinition category(EvaluatorFunctionCategory category) {
            this.category = category;

            return this;
        }

        public ModifiableEvaluatorFunctionDefinition parameters(ModifiableEvaluatorFunctionParameter... parameters) {
            this.parameters = List.of(parameters);

            return this;
        }

        public ModifiableEvaluatorFunctionDefinition returnType(EvaluatorFunctionType returnType) {
            this.returnType = returnType;

            return this;
        }

        public ModifiableEvaluatorFunctionDefinition example(String example) {
            this.example = example;

            return this;
        }

        /**
         * Builds an immutable {@link EvaluatorFunctionDefinition} from this builder's current state.
         *
         * @return a new {@link EvaluatorFunctionDefinition}
         */
        public EvaluatorFunctionDefinition toDefinition() {
            List<EvaluatorFunctionDefinition.EvaluatorFunctionParameter> builtParameters = parameters.stream()
                .map(ModifiableEvaluatorFunctionParameter::toParameter)
                .toList();

            return new EvaluatorFunctionDefinition(
                name, title, description, category, Collections.unmodifiableList(builtParameters), returnType, example);
        }
    }

    /**
     * Mutable builder for {@link EvaluatorFunctionDefinition.EvaluatorFunctionParameter}.
     *
     * @author Ivica Cardic
     */
    public static final class ModifiableEvaluatorFunctionParameter {

        private String description;
        private final String name;
        private boolean required = true;
        private EvaluatorFunctionType type;

        private ModifiableEvaluatorFunctionParameter(String name) {
            this.name = name;
        }

        public ModifiableEvaluatorFunctionParameter description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableEvaluatorFunctionParameter type(EvaluatorFunctionType type) {
            this.type = type;

            return this;
        }

        public ModifiableEvaluatorFunctionParameter required(boolean required) {
            this.required = required;

            return this;
        }

        EvaluatorFunctionDefinition.EvaluatorFunctionParameter toParameter() {
            return new EvaluatorFunctionDefinition.EvaluatorFunctionParameter(name, description, type, required);
        }
    }
}
