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

package com.bytechef.component.sendgrid.util;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.ATTACHMENTS;
import static java.util.Base64.Encoder;
import static java.util.Base64.getEncoder;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.objects.Attachments;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marko Krišković
 */
public class SendgridUtils {
    private static final Encoder ENCODER = getEncoder();

    private SendgridUtils() {
    }

    public static List<Attachments> getAttachments(Parameters inputParameters, ActionContext actionContext) {
        List<Attachments> attachments = new ArrayList<>();

        List<FileEntry> fileEntries = inputParameters.getFileEntries(ATTACHMENTS, List.of());
        for (FileEntry fileEntry : fileEntries) {
            Attachments attachment = new Attachments.Builder(fileEntry.getName(),
                ENCODER.encodeToString(actionContext.file(file -> file.readAllBytes(fileEntry))))
                    .build();

            attachments.add(attachment);
        }

        return attachments;
    }

    public static List<Option<String>> getTemplates(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {
        Request request = new Request();

        request.setMethod(Method.GET);
        request.setEndpoint("templates");
        request.addQueryParam("generations", "dynamic"); // value: "legacy, dynamic"

        SendGrid sg = new SendGrid(connectionParameters.getRequiredString(TOKEN));
        Response response = sg.api(request);
        String json = response.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(json);
        JsonNode path = node.path("templates");

        List<Option<String>> options = new ArrayList<>();
        for (JsonNode template : path) {
            options.add(option(template.path("name")
                .textValue(),
                template.path("id")
                    .textValue()));
        }

        return options;
    }
}
