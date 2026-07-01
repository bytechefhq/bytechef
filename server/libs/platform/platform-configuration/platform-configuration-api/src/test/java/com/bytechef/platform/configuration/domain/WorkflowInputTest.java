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

package com.bytechef.platform.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.configuration.domain.WorkflowInput.ComponentInputReference;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowInputTest {

    @Test
    void testPlainInputHasNoComponentReference() {
        WorkflowInput workflowInput = new WorkflowInput(new Workflow.Input("email", "Email", "string", true));

        assertEquals("email", workflowInput.getName());
        assertNull(workflowInput.getComponentInputReference());
        assertNull(workflowInput.getObjectName());
        assertFalse(workflowInput.isInternalOnly());
    }

    @Test
    void testComponentInputReferenceReadFromExtensions() {
        WorkflowInput workflowInput = new WorkflowInput(
            new Workflow.Input(
                "dateRange", "Date Range", "string", false,
                Map.of("componentName", "googleSheets", "componentVersion", 2, "groupName", "sheetSelection")));

        ComponentInputReference componentInputReference = workflowInput.getComponentInputReference();

        assertEquals("googleSheets", componentInputReference.componentName());
        assertEquals(2, componentInputReference.componentVersion());
        assertEquals("sheetSelection", componentInputReference.groupName());
    }

    @Test
    void testObjectNameAndInternalOnlyReadFromExtensions() {
        WorkflowInput workflowInput = new WorkflowInput(
            new Workflow.Input(
                "contactMapping", "Contact Mapping", "field_mapping", false,
                Map.of("objectName", "Contacts", "internalOnly", true)));

        assertEquals("Contacts", workflowInput.getObjectName());
        assertTrue(workflowInput.isInternalOnly());
    }

    @Test
    void testComponentInputReferenceRejectsMissingGroupName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ComponentInputReference("googleSheets", 2, null));
    }
}
