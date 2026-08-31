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

package com.bytechef.component.browser.use.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Marija Horvat
 */
public class BrowserUseConstants {

    public static final String CURSOR = "cursor";
    public static final String ENABLE_SCHEDULED_TASKS = "enableScheduledTasks";
    public static final String FORMAT = "format";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String LIMIT = "limit";
    public static final String MODEL = "model";
    public static final String OUTPUT_SCHEMA = "outputSchema";
    public static final String PAGE = "page";
    public static final String PAGE_SIZE = "page_size";
    public static final String RUN_ID = "runId";
    public static final String SCHEMA = "schema";
    public static final String SESSION_ID = "sessionId";
    public static final String SKILLS = "skills";
    public static final String TASK = "task";

    public static final ModifiableObjectProperty OUTPUT_PROPERTY = object(OUTPUT_SCHEMA)
        .label("Output")
        .description("Configure how the browser agent should return results.")
        .properties(
            string(FORMAT)
                .label("Output Format")
                .options(
                    option("Text", "TEXT", "Return plain text."),
                    option("Structured Data", "JSON", "Return structured JSON data."))
                .defaultValue("TEXT")
                .required(true),
            string(SCHEMA)
                .label("Output Schema")
                .description("Define the structure of data to extract from the browser task.")
                .controlType(JSON_SCHEMA_BUILDER)
                .displayCondition("outputSchema.format == 'JSON'")
                .required(true))
        .required(true);

    public static final ModifiableObjectProperty SESSION_RESPONSE_PROPERTY = object()
        .properties(
            string("id")
                .description("Unique session identifier."),
            string("status")
                .description("Current session lifecycle status."),
            string("model")
                .description("The model tier used for this session."),
            dateTime("createdAt")
                .description("When the session was created."),
            dateTime("updatedAt")
                .description("When the session was last updated."),
            string("title")
                .description("Auto-generated short title summarizing the task."),
            object("output")
                .description("The agent's final output."),
            object("outputSchema")
                .description("The JSON Schema that was requested for structured output, if any."),
            integer("stepCount")
                .description("Number of steps the agent has executed so far."),
            string("lastStepSummary")
                .description("Human-readable summary of the most recent agent step."),
            bool("isTaskSuccessful")
                .description("Whether the task completed successfully."),
            string("liveUrl")
                .description("URL to view the live browser session."),
            array("recordingUrls")
                .items(string())
                .description("URLs to download session recordings."),
            string("profileId")
                .description("ID of the browser profile loaded in this session, if any."),
            string("workspaceId")
                .description("ID of the workspace attached to this session, if any."),
            string("proxyCountryCode")
                .description("Country code of the proxy used for this session, or null if no proxy."),
            string("maxCostUsd")
                .description("Maximum cost limit in USD set for this session."),
            integer("totalInputTokens")
                .description("Total LLM input tokens consumed by this session."),
            integer("totalOutputTokens")
                .description("Total LLM output tokens consumed by this session."),
            string("proxyUsedMb")
                .description("Proxy bandwidth used in megabytes."),
            string("llmCostUsd")
                .description("Cost of LLM usage in USD."),
            string("proxyCostUsd")
                .description("Cost of proxy bandwidth in USD."),
            string("browserCostUsd")
                .description("Cost of browser compute time in USD."),
            string("totalCostUsd")
                .description("Total session cost in USD (LLM + proxy + browser)."),
            string("screenshotUrl")
                .description("URL of the latest browser screenshot."),
            string("agentmailEmail")
                .description("Temporary email address provisioned for this session (via AgentMail)."));

    public static final ModifiableObjectProperty RUN_CREATE_RESPONSE_PROPERTY = object()
        .properties(
            string(ID)
                .description("Unique run identifier."),
            string("status")
                .description("Current run status."),
            string(MODEL)
                .description("Model used for the run."),
            string(SESSION_ID)
                .description("Conversation session used by the run."),
            string("workspaceId")
                .description("Workspace used by the run."),
            string("eventsUrl")
                .description("Relative URL for the run event stream."),
            array("missingFileIds")
                .items(string())
                .description("Attached file IDs that were not found."));

    public static final ModifiableObjectProperty RUN_RESPONSE_PROPERTY = object()
        .properties(
            string(ID)
                .description("Unique run identifier."),
            string(TASK)
                .description("Natural-language task sent to the agent."),
            string("title")
                .description("Auto-generated run title."),
            string(MODEL)
                .description("Model used for the run."),
            integer("contextLimit")
                .description("Maximum model context available to the run."),
            string("status")
                .description("Current run status."),
            string("result")
                .description("Final result after the run completes."),
            string("error")
                .description("Error message when the run fails."),
            string(SESSION_ID)
                .description("Conversation session used by the run."),
            string("workspaceId")
                .description("Workspace used by the run."),
            array("attachedFileIds")
                .items(string())
                .description("File IDs attached to the run."),
            object("judgement")
                .description("Optional judge result for the run."),
            integer("totalInputTokens")
                .description("Total LLM input tokens consumed by the run."),
            integer("totalOutputTokens")
                .description("Total LLM output tokens consumed by the run."),
            string("totalCostUsd")
                .description("Total run cost in USD."),
            dateTime("createdAt")
                .description("When the run was created."),
            dateTime("updatedAt")
                .description("When the run was last updated."));

    private BrowserUseConstants() {
    }
}
