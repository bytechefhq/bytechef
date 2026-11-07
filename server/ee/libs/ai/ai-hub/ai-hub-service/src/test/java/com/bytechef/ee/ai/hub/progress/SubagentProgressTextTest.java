/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SubagentProgressTextTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "getWorkflowBuildInstructions | {} | Reading the build instructions",
            "getTaskDispatcherBuildInstructions | {} | Reading the build instructions",
            "searchTasks | {\"query\":\"gmail\"} | Searching components",
            "searchComponents | {\"query\":\"slack\"} | Searching components",
            "listTriggers | {} | Searching components",
            "getTask | {\"type\":\"trigger\",\"name\":\"newEmail\",\"componentName\":\"gmail\"} | Adding the trigger \"gmail › newEmail\"",
            "getTaskProperties | {\"type\":\"action\",\"name\":\"sendChannelMessage\",\"componentName\":\"slack\"} | Configuring \"slack › sendChannelMessage\"",
            "getTaskDefinition | {\"type\":\"taskDispatcher\",\"name\":\"condition\"} | Adding a \"condition\" step",
            "getTask | {\"type\":\"clusterElement\",\"name\":\"model\",\"componentName\":\"openAi\"} | Configuring \"openAi › model\"",
            "getTask | {\"type\":\"taskDispatcher\"} | Adding a flow step",
            "getTask | {} | Configuring a step",
            "getTrigger | {\"componentName\":\"gmail\",\"triggerName\":\"newEmail\"} | Adding the trigger \"gmail › newEmail\"",
            "getAction | {\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\"} | Configuring \"slack › sendChannelMessage\"",
            "getClusterElement | {\"componentName\":\"openAi\",\"clusterElementName\":\"model\"} | Configuring \"openAi › model\"",
            "getProperties | {\"componentName\":\"slack\",\"operationName\":\"sendChannelMessage\"} | Configuring \"slack › sendChannelMessage\"",
            "getComponent | {\"componentName\":\"gmail\"} | Looking at \"gmail\"",
            "lookupPropertyOptions | {\"componentName\":\"slack\",\"operationName\":\"sendChannelMessage\"} | Looking up options for \"slack › sendChannelMessage\"",
            "validateTask | {\"componentName\":\"slack\",\"name\":\"sendChannelMessage\"} | Checking \"slack › sendChannelMessage\"",
            "validateWorkflow | {} | Validating the workflow",
            "getWorkflow | {} | Reading the workflow",
            "searchWorkflows | {} | Looking up workflows",
            "createProjectWorkflow | {\"projectId\":1} | Creating the workflow",
            "updateWorkflow | {\"workflowId\":\"w-1\",\"definition\":\"{}\"} | Saving the workflow",
            "deleteWorkflow | {} | Removing a workflow",
            "saveWorkflowTestConnection | {\"workflowId\":\"w-1\",\"workflowNodeName\":\"sendChannelMessage_1\",\"connectionKey\":\"slack\",\"connectionId\":12} | Connecting \"slack\"",
            "saveWorkflowTestConnection | {\"workflowNodeName\":\"model_1\",\"connectionId\":12} | Connecting \"model_1\"",
            "updateScriptComponentCode | {} | Writing the script code",
            "listDataTables | {} | Looking up data tables",
            "createDataTable | {} | Creating a data table",
            "askUserQuestion | {} | Asking you a question",
            "braveWebSearch | {\"query\":\"x\"} | Brave web search",
            "fetch_page | {} | Fetch page"
        })
    void testDescribesEachSpecialistToolInPlainWords(String toolName, String toolInput, String expected) {
        assertThat(SubagentProgressText.describeStep(toolName, toolInput)).isEqualTo(expected);
    }

    @Test
    void testDescribesAStepWhoseInputIsNotJson() {
        assertThat(SubagentProgressText.describeStep("getTask", "not json")).isEqualTo("Configuring a step");
        assertThat(SubagentProgressText.describeStep("searchTasks", null)).isEqualTo("Searching components");
    }

    @Test
    void testNeverPrintsTheConnectionIdOrALongName() {
        String text = SubagentProgressText.describeStep(
            "saveWorkflowTestConnection",
            "{\"connectionKey\":\"a very long connection key that goes on and on and on\",\"connectionId\":987654}");

        assertThat(text).doesNotContain("987654")
            .endsWith("…\"");
        assertThat(text.length()).isLessThanOrEqualTo(SubagentProgressText.MAX_LENGTH);
    }

    @Test
    void testLabelsARequestByItsFirstSentenceCappedAtTheMaximumLength() {
        assertThat(SubagentProgressText.describeRequest("{\"request\":\"Build a workflow\\n\\nthat posts to Slack\"}"))
            .isEqualTo("Build a workflow");

        String label = SubagentProgressText.describeRequest(
            "{\"request\":\"" + "Build a workflow that copies every new spreadsheet row into a CRM contact ".repeat(3) +
                "\"}");

        assertThat(label).hasSize(SubagentProgressText.MAX_LENGTH)
            .endsWith("…");
    }

    @Test
    void testLabelsAnImportByItsInstructionWithoutTheSourceDefinition() {
        String label = SubagentProgressText.describeRequest(
            "{\"request\":\"Convert this n8n workflow: {\\\"nodes\\\":[{\\\"apiKey\\\":\\\"sk-live-1\\\"}]}\"," +
                "\"projectId\":3}");

        assertThat(label).isEqualTo("Convert this n8n workflow");
    }

    @Test
    void testMasksCredentialsInARequestLabel() {
        assertThat(SubagentProgressText.describeRequest("{\"request\":\"Use api_key=sk-12345 to call it\"}"))
            .isEqualTo("Use api_key=••• to call it");
        assertThat(SubagentProgressText.describeRequest("{\"request\":\"Use password: hunter2 for login\"}"))
            .isEqualTo("Use password: ••• for login");
        assertThat(
            SubagentProgressText.describeRequest("{\"request\":\"Call it with ghp_abcdefghijklmnopqrstuvwxyz0123\"}"))
                .isEqualTo("Call it with •••");
        assertThat(SubagentProgressText.describeRequest("{\"request\":\"Refresh the token every hour\"}"))
            .isEqualTo("Refresh the token every hour");
    }

    @Test
    void testLabelsNothingWithoutARequest() {
        assertThat(SubagentProgressText.describeRequest("{\"workflowId\":\"w-1\"}")).isEmpty();
        assertThat(SubagentProgressText.describeRequest("not json")).isEmpty();
        assertThat(SubagentProgressText.describeRequest(null)).isEmpty();
        assertThat(SubagentProgressText.describeRequest("{\"description\":\"Summarise the docs\"}"))
            .isEqualTo("Summarise the docs");
    }
}
