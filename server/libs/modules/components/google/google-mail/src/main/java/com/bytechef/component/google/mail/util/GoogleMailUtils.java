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

import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class GoogleMailUtils {

    private GoogleMailUtils() {
    }

    public static List<Option<String>> getLabelIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        List<Option<String>> options = new ArrayList<>();

        List<Label> labels = gmail
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
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        Gmail service = GoogleServices.getMail(connectionParameters);

        List<Message> messages = service
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
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        List<Thread> threads = gmail
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
}
