/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.task.handler.httpclient;

import static com.integri.atlas.task.descriptor.model.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.createTaskAuthDescriptor;
import static com.integri.atlas.task.descriptor.model.DSL.option;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.*;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.ADD_TO;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.AuthType.API_KEY;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.AuthType.BASIC_AUTH;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.AuthType.BEARER_TOKEN;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.AuthType.DIGEST_AUTH;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.AuthType.OAUTH2;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.HTTP_CLIENT;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.KEY;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.PASSWORD;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.TOKEN;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.USERNAME;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.VALUE;

import com.integri.atlas.task.descriptor.handler.TaskAuthDescriptorHandler;
import com.integri.atlas.task.descriptor.model.DSL;
import com.integri.atlas.task.descriptor.model.TaskAuthDescriptor;
import com.integri.atlas.task.descriptor.model.TaskAuthDescriptors;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class HttpClientTaskAuthDescriptorHandler implements TaskAuthDescriptorHandler {

    private static final List<TaskAuthDescriptor> TASK_AUTH_DESCRIPTORS = List.of(
        createTaskAuthDescriptor(API_KEY.name().toLowerCase())
            .displayName("API Key")
            .properties(
                STRING_PROPERTY(KEY).displayName("Key").required(true).defaultValue("api_token"),
                STRING_PROPERTY(VALUE).displayName("Value").required(true),
                STRING_PROPERTY(ADD_TO)
                    .displayName("Add to")
                    .required(true)
                    .options(
                        option("Header", ApiTokenLocation.HEADER.name()),
                        option("QueryParams", ApiTokenLocation.QUERY_PARAMS.name())
                    )
                    .required(true)
            ),
        createTaskAuthDescriptor(BEARER_TOKEN.name().toLowerCase())
            .displayName("Bearer Token")
            .properties(STRING_PROPERTY(TOKEN).displayName("Token").required(true)),
        createTaskAuthDescriptor(BASIC_AUTH.name().toLowerCase())
            .displayName("Basic Auth")
            .properties(
                STRING_PROPERTY(USERNAME).displayName("Username").required(true),
                STRING_PROPERTY(PASSWORD).displayName("Password").required(true)
            ),
        createTaskAuthDescriptor(DIGEST_AUTH.name().toLowerCase())
            .displayName("Digest Auth")
            .properties(
                STRING_PROPERTY(USERNAME).displayName("Username").required(true),
                STRING_PROPERTY(PASSWORD).displayName("Password").required(true)
            ),
        createTaskAuthDescriptor(OAUTH2.name().toLowerCase()).displayName("OAuth2").properties()
    );

    @Override
    public TaskAuthDescriptors getTaskAuthDescriptors() {
        return DSL.createTaskAuthDescriptors(HTTP_CLIENT, TASK_AUTH_DESCRIPTORS);
    }
}
