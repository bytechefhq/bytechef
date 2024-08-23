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

package com.bytechef.component.google.mail.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ATTACHMENTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HISTORY_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.INTERNAL_DATE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MINIMAL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NAME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.PAYLOAD;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.RAW;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIMPLE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIZE_ESTIMATE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SNIPPET;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.THREAD_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionOutputFunction;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OutputResponse;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleMailUtils {

    protected static final ModifiableObjectProperty PARSED_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(SUBJECT),
            string(FROM),
            string(TO),
            string("body_plain"),
            string("body_html"),
            array(ATTACHMENTS).items(fileEntry()));

    protected static final ModifiableObjectProperty RAW_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(HISTORY_ID),
            string(ID),
            number(INTERNAL_DATE),
            array(LABEL_IDS)
                .items(string()),
            string(RAW),
            integer(SIZE_ESTIMATE),
            string(SNIPPET),
            string(THREAD_ID));

    protected static final ModifiableObjectProperty MINIMAL_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(HISTORY_ID),
            string(ID),
            number(INTERNAL_DATE),
            array(LABEL_IDS).items(string()),
            integer(SIZE_ESTIMATE),
            string(SNIPPET),
            string(THREAD_ID));

    protected static final ModifiableObjectProperty METADATA_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(HISTORY_ID),
            string(ID),
            number(INTERNAL_DATE),
            array(LABEL_IDS).items(string()),
            object(PAYLOAD)
                .properties(
                    array(HEADERS)
                        .items(
                            object()
                                .properties(
                                    string(NAME),
                                    string(VALUE)))),
            integer(SIZE_ESTIMATE),
            string(SNIPPET),
            string(THREAD_ID));

    private GoogleMailUtils() {
    }

    public static List<Option<String>> getLabelIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context)
        throws IOException {

        List<Option<String>> options = new ArrayList<>();

        List<Label> labels = GoogleServices.getMail(connectionParameters)
            .users()
            .labels()
            .list("me")
            .execute()
            .getLabels();

        for (Label label : labels) {
            options.add(option(label.getName(), label.getName()));
        }

        return options;
    }

    public static List<Option<String>> getMessageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context)
        throws IOException {

        List<Message> messages = GoogleServices.getMail(connectionParameters)
            .users()
            .messages()
            .list("me")
            .execute()
            .getMessages();

        List<Option<String>> options = new ArrayList<>();

        for (Message message : messages) {
            options.add(option(message.getId(), message.getId()));
        }

        return options;
    }

    public static List<Option<String>> getThreadIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) throws IOException {

        List<Thread> threads = GoogleServices.getMail(connectionParameters)
            .users()
            .threads()
            .list("me")
            .execute()
            .getThreads();

        List<Option<String>> options = new ArrayList<>();

        for (Thread thread : threads) {
            options.add(option(thread.getId(), thread.getId()));
        }

        return options;
    }

    public static SingleConnectionOutputFunction getOutput() {
        return (inputParameters, connectionParameters, context) -> {

            String format = inputParameters.getRequiredString(FORMAT);

            ModifiableObjectProperty bodyPlain = switch (format) {
                case SIMPLE -> PARSED_MESSAGE_OUTPUT_PROPERTY;
                case RAW -> RAW_MESSAGE_OUTPUT_PROPERTY;
                case MINIMAL -> MINIMAL_MESSAGE_OUTPUT_PROPERTY;
                case METADATA -> METADATA_MESSAGE_OUTPUT_PROPERTY;
                default -> FULL_MESSAGE_OUTPUT_PROPERTY;
            };

            return new OutputResponse(bodyPlain);
        };
    }
}
