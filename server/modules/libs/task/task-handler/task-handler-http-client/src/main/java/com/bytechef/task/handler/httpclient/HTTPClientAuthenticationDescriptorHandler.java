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

package com.bytechef.task.handler.httpclient;

import static com.bytechef.hermes.descriptor.domain.DSL.STRING_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.createAuthenticationDescriptor;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.*;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.ADD_TO;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.KEY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.PASSWORD;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.TOKEN;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.USERNAME;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.VALUE;

import com.bytechef.hermes.descriptor.domain.AuthenticationDescriptor;
import com.bytechef.hermes.descriptor.domain.AuthenticationDescriptors;
import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandler;
import com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.ApiTokenLocation;
import com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.AuthType;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class HTTPClientAuthenticationDescriptorHandler implements AuthenticationDescriptorHandler {

    private static final List<AuthenticationDescriptor> TASK_AUTH_DESCRIPTORS = List.of(
            createAuthenticationDescriptor(AuthType.API_KEY.name().toLowerCase())
                    .displayName("API Key")
                    .properties(
                            STRING_PROPERTY(KEY)
                                    .displayName("Key")
                                    .required(true)
                                    .defaultValue("api_token"),
                            STRING_PROPERTY(VALUE).displayName("Value").required(true),
                            STRING_PROPERTY(ADD_TO)
                                    .displayName("Add to")
                                    .required(true)
                                    .options(
                                            DSL.option("Header", ApiTokenLocation.HEADER.name()),
                                            DSL.option("QueryParams", ApiTokenLocation.QUERY_PARAMS.name()))
                                    .required(true)),
            createAuthenticationDescriptor(AuthType.BEARER_TOKEN.name().toLowerCase())
                    .displayName("Bearer Token")
                    .properties(STRING_PROPERTY(TOKEN).displayName("Token").required(true)),
            createAuthenticationDescriptor(AuthType.BASIC_AUTH.name().toLowerCase())
                    .displayName("Basic Auth")
                    .properties(
                            STRING_PROPERTY(USERNAME).displayName("Username").required(true),
                            STRING_PROPERTY(PASSWORD).displayName("Password").required(true)),
            createAuthenticationDescriptor(AuthType.DIGEST_AUTH.name().toLowerCase())
                    .displayName("Digest Auth")
                    .properties(
                            STRING_PROPERTY(USERNAME).displayName("Username").required(true),
                            STRING_PROPERTY(PASSWORD).displayName("Password").required(true)),
            createAuthenticationDescriptor(AuthType.OAUTH2.name().toLowerCase())
                    .displayName("OAuth2")
                    .properties(
                            STRING_PROPERTY(ACCESS_TOKEN)
                                    .displayName("Access Token")
                                    .required(true),
                            STRING_PROPERTY(HEADER_PREFIX)
                                    .displayName("Header Prefix")
                                    .required(true)
                                    .defaultValue("Bearer")));

    @Override
    public AuthenticationDescriptors getAuthenticationDescriptors() {
        return DSL.createAuthenticationDescriptors(HTTP_CLIENT, TASK_AUTH_DESCRIPTORS);
    }
}
