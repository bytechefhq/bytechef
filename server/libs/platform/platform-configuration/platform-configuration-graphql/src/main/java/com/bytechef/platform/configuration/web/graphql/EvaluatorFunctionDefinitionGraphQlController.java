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

package com.bytechef.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.evaluator.EvaluatorFunctionDefinition;
import com.bytechef.evaluator.EvaluatorFunctionDefinitionFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class EvaluatorFunctionDefinitionGraphQlController {

    private final List<EvaluatorFunctionDefinition> evaluatorFunctionDefinitions;

    public EvaluatorFunctionDefinitionGraphQlController(
        List<EvaluatorFunctionDefinitionFactory> evaluatorFunctionDefinitionFactories) {

        this.evaluatorFunctionDefinitions = evaluatorFunctionDefinitionFactories.stream()
            .flatMap(factory -> factory.getDefinitions()
                .stream())
            .toList();
    }

    @QueryMapping
    public EvaluatorFunctionDefinition evaluatorFunctionDefinition(@Argument String name) {
        return evaluatorFunctionDefinitions.stream()
            .filter(definition -> definition.name()
                .equals(name))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Evaluator function not found: " + name));
    }

    @QueryMapping
    public List<EvaluatorFunctionDefinition> evaluatorFunctionDefinitions(@Argument String name) {
        if (name == null || name.isBlank()) {
            return evaluatorFunctionDefinitions;
        }

        String lowercaseName = name.toLowerCase();

        return evaluatorFunctionDefinitions.stream()
            .filter(definition -> definition.name()
                .toLowerCase()
                .contains(lowercaseName) ||
                definition.title()
                    .toLowerCase()
                    .contains(lowercaseName))
            .toList();
    }
}
