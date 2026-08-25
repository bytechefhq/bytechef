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

package com.bytechef.platform.workflow.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.workflow.validator.exception.WorkflowValidatorErrorType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Covers the reserved-name guards -- {@link WorkflowValidatorFacade#validateNoReservedInputNames} for
 * {@code inputs[].name} and {@link WorkflowValidatorFacade#validateNoReservedNodeNames} for top-level trigger/task node
 * names -- that protect the reserved {@code __triggerName} and {@code vars} job-input keys from ever being shadowed by
 * a workflow-authored name.
 */
class WorkflowValidatorReservedInputNameTest {

    private final WorkflowValidatorFacade workflowValidatorFacade = reservedNameFacade();

    @Test
    void testReservedInputNameRejected() {
        String definition = """
            {"label":"t","inputs":[{"name":"__triggerName","type":"string"}],"tasks":[]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testOrdinaryInputNameAccepted() {
        String definition = """
            {"label":"t","inputs":[{"name":"message","type":"string"}],"tasks":[]}
            """;

        assertThatCode(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void validateNoReservedInputNamesThrowsWithErrorKeyOnReservedName() {
        String definition = """
            {"label":"t","inputs":[{"name":"__secret","type":"string"}],"tasks":[]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(WorkflowValidatorErrorType.RESERVED_INPUT_NAME.getErrorKey());
    }

    @Test
    void validateNoReservedInputNamesPassesWhenInputsMissing() {
        String definition = """
            {"label":"t","tasks":[]}
            """;

        assertThatCode(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void validateNoReservedInputNamesFailsOpenOnMalformedJson() {
        String definition = "not json";

        assertThatCode(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void validateNoReservedNodeNamesRejectsReservedTriggerName() {
        String definition = """
            {"label":"t","triggers":[{"name":"__triggerName","type":"manual/v1/manual"}],"tasks":[]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(WorkflowValidatorErrorType.RESERVED_NODE_NAME.getErrorKey());
    }

    @Test
    void validateNoReservedNodeNamesRejectsReservedTaskName() {
        String definition = """
            {"label":"t","triggers":[],"tasks":[{"name":"__internal","type":"logger/v1/info"}]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void validateNoReservedNodeNamesAcceptsOrdinaryNames() {
        String definition = """
            {"label":"t","triggers":[{"name":"trigger_1","type":"manual/v1/manual"}],
             "tasks":[{"name":"task_1","type":"logger/v1/info"}]}
            """;

        assertThatCode(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void validateNoReservedNodeNamesFailsOpenOnMalformedJson() {
        String definition = "not json";

        assertThatCode(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateNoReservedInputNamesRejectsVars() {
        String definition = """
            {"label":"t","inputs":[{"name":"vars","type":"string"}],"tasks":[]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(WorkflowValidatorErrorType.RESERVED_INPUT_NAME.getErrorKey());
    }

    @Test
    void testValidateNoReservedInputNamesAllowsVarsPrefixedNames() {
        String definition = """
            {"label":"t","inputs":[{"name":"varsCount","type":"string"}],"tasks":[]}
            """;

        assertThatCode(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateNoReservedNodeNamesRejectsVars() {
        String definition = """
            {"label":"t","triggers":[],"tasks":[{"name":"vars","type":"var/v1/set"}]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(WorkflowValidatorErrorType.RESERVED_NODE_NAME.getErrorKey());
    }

    /**
     * A minimal {@link WorkflowValidatorFacade} that exercises the real {@code validateNoReservedInputNames} and
     * {@code validateNoReservedNodeNames} default methods, without resolving component/trigger definitions.
     */
    private static WorkflowValidatorFacade reservedNameFacade() {
        return new WorkflowValidatorFacade() {

            @Override
            public WorkflowValidationResult validateWorkflow(String workflow) {
                return new WorkflowValidationResult(List.of(), List.of());
            }

            @Override
            public WorkflowValidationResult validateWorkflowById(String workflowId) {
                return new WorkflowValidationResult(List.of(), List.of());
            }

            @Override
            public List<String> getDuplicateNodeNames(String workflow) {
                return List.of();
            }
        };
    }
}
