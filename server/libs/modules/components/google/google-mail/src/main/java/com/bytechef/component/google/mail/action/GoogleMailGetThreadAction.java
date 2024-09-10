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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HISTORY_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MESSAGES;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIMPLE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SNIPPET;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getMessageOutputProperty;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getSimpleMessage;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.component.google.mail.util.GoogleMailUtils.SimpleMessage;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleMailGetThreadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getThread")
        .title("Get Thread")
        .description("Gets the specified thread.")
        .properties(
            string(ID)
                .label("Thread ID")
                .description("The ID of the thread to retrieve.")
                .options((ActionOptionsFunction<String>) GoogleMailUtils::getThreadIdOptions)
                .required(true),
            FORMAT_PROPERTY,
            METADATA_HEADERS_PROPERTY)
        .output(GoogleMailGetThreadAction::getOutput)
        .perform(GoogleMailGetThreadAction::perform);

    private GoogleMailGetThreadAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        String format = inputParameters.getRequiredString(FORMAT);

        Thread thread = getThread(inputParameters, gmail, format);

        if (format.equals(SIMPLE)) {
            List<SimpleMessage> simpleMessages = new ArrayList<>();

            for (Message message : thread.getMessages()) {
                simpleMessages.add(getSimpleMessage(message, actionContext, gmail));
            }

            return new ThreadCustom(thread.getId(), thread.getSnippet(), thread.getHistoryId(), simpleMessages);
        } else {
            return thread;
        }
    }

    private static Thread getThread(Parameters inputParameters, Gmail gmail, String format) throws IOException {
        return gmail.users()
            .threads()
            .get(ME, inputParameters.getRequiredString(ID))
            .setFormat(format.equals(SIMPLE) ? FULL : format)
            .setMetadataHeaders(inputParameters.getList(METADATA_HEADERS, String.class, List.of()))
            .execute();
    }

    @SuppressFBWarnings("EI")
    protected record ThreadCustom(
        String id, String snippet, BigInteger historyId, List<SimpleMessage> messages) {
    }

    public static OutputResponse getOutput(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return new OutputResponse(
            object()
                .properties(
                    string(ID),
                    string(SNIPPET),
                    string(HISTORY_ID),
                    array(MESSAGES)
                        .items(getMessageOutputProperty(inputParameters.getRequiredString(FORMAT)))));
    }
}
