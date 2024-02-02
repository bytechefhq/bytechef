/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.openai.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.openai.constant.OpenAIConstants.CREATE_ASSISTANT;
import static com.bytechef.component.openai.constant.OpenAIConstants.DESCRIPTION;
import static com.bytechef.component.openai.constant.OpenAIConstants.FILE_IDS;
import static com.bytechef.component.openai.constant.OpenAIConstants.FUNCTION;
import static com.bytechef.component.openai.constant.OpenAIConstants.INSTRUCTIONS;
import static com.bytechef.component.openai.constant.OpenAIConstants.METADATA;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.NAME;
import static com.bytechef.component.openai.constant.OpenAIConstants.PARAMETERS;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOOLS;
import static com.bytechef.component.openai.constant.OpenAIConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.assistants.AssistantRequest;
import com.theokanning.openai.service.OpenAiService;

/**
 * @author Monika Domiter
 */
public class OpenAICreateAssistantAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_ASSISTANT)
        .title("Create assistant")
        .description("Create an assistant with a model and instructions.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true),
            string(NAME)
                .label("Name")
                .description("The name of the assistant.")
                .maxLength(256)
                .required(false),
            string(DESCRIPTION)
                .label("Description")
                .description("The description of the assistant.")
                .maxLength(512)
                .required(false),
            string(INSTRUCTIONS)
                .label("Instructions")
                .description("The system instructions that the assistant uses.")
                .maxLength(32768)
                .required(false),
            array(TOOLS)
                .label("Tools")
                .description("A list of tool enabled on the assistant.")
                .items(
                    object().properties(
                        string(TYPE)
                            .label("Type")
                            .description("The type of tool being defined.")
                            .options(
                                option("Code interpreter", "code_interpreter"),
                                option("Retrieval", "retrieval"),
                                option("Function", FUNCTION))
                            .required(true),
                        object(FUNCTION)
                            .label("Function")
                            .displayCondition("%s === '%s'".formatted(TYPE, FUNCTION))
                            .properties(
                                string(DESCRIPTION)
                                    .label("Description")
                                    .description(
                                        "A description of what the function does, used by the model to choose when " +
                                            "and how to call the function.")
                                    .required(false),
                                string(NAME)
                                    .label("Name")
                                    .description("The name of the function to be called.")
                                    .maxLength(64)
                                    .required(true),
                                object(PARAMETERS)
                                    .label("Parameters")
                                    .description(
                                        "The parameters the functions accepts, described as a JSON Schema object.")
                                    .required(true))
                            .required(false))

                )
                .required(false),
            array(FILE_IDS)
                .label("File ids")
                .description("A list of file IDs attached to this assistant. ")
                .required(false),
            object(METADATA)
                .label("Metadata")
                .description(
                    "Set of 16 key-value pairs that can be attached to an object. This can be useful for storing " +
                        "additional information about the object in a structured format. Keys can be a maximum of 64 " +
                        "characters long and values can be a maxium of 512 characters long.")
                .additionalProperties(
                    string())
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string("id"),
                    string("object"),
                    integer("createdAt"),
                    string("name"),
                    string("description"),
                    string("model"),
                    string("instructions"),
                    array("tools")
                        .items(
                            object()
                                .properties(
                                    string("type"),
                                    object("function")
                                        .properties(
                                            string("description"),
                                            string("name"),
                                            object("parameters")))),
                    array("fileIds")
                        .items(
                            string("fileId")),
                    object("metadata"))

        )
        .perform(OpenAICreateAssistantAction::perform);

    private OpenAICreateAssistantAction() {
    }

    public static Assistant perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        OpenAiService openAiService = new OpenAiService((String) connectionParameters.get(TOKEN));

        AssistantRequest assistantRequest = new AssistantRequest();

        assistantRequest.setModel(inputParameters.getRequiredString(MODEL));
        assistantRequest.setName(inputParameters.getString(NAME));
        assistantRequest.setDescription(inputParameters.getString(DESCRIPTION));
        assistantRequest.setInstructions(inputParameters.getString(INSTRUCTIONS));
        assistantRequest.setTools(inputParameters.getList(TOOLS, new TypeReference<>() {}));
        assistantRequest.setFileIds(inputParameters.getList(FILE_IDS, new TypeReference<>() {}));
        assistantRequest.setMetadata(inputParameters.getMap(METADATA, new TypeReference<>() {}));

        return openAiService.createAssistant(assistantRequest);
    }
}
