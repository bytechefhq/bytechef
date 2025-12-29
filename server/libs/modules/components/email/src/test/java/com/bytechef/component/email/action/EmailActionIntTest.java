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

package com.bytechef.component.email.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.email.EmailProtocol;
import com.bytechef.component.email.constant.EmailConstants;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.ServerSetupTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Igor Beslic
 */
public class EmailActionIntTest {

    static {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP)
        .withConfiguration(GreenMailConfiguration
            .aConfig()
            .withUser("bytecheftest@bytechef.io", "bytecheftest", "bytecheftest"));

    @Test
    public void testEmailActions() throws Exception {
        ActionContext actionContext = mock(ActionContext.class);
        when(actionContext.json(any()))
            .thenReturn(JsonUtils.write("test.from@test.com"));

        AbstractServer server = greenMail.getSmtp();

        SendEmailAction.perform(
            getSendEmailActionParameters(server.getPort()), getConnectionParameters(), actionContext);

        greenMail.waitForIncomingEmail(1);

        server = greenMail.getImap();

        Object result = ReadEmailAction.perform(
            getReceieveEmailActionParameters(server.getPort()), getConnectionParameters(), actionContext);

        Class<?> clazz = result.getClass();

        Assertions.assertTrue(clazz.isArray());

        Map<?, ?>[] resultMap = (Map<?, ?>[]) result;

        Assertions.assertEquals(1, resultMap.length);

        Object fromObject = resultMap[0].get("from");

        String from = (String) fromObject;

        Assertions.assertTrue(from.contains("\"test.from@test.com\""));

        System.out.println(result);
    }

    private Parameters getConnectionParameters() {
        HashMap<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(EmailConstants.HOST, "localhost");
        parameterMap.put(Authorization.USERNAME, "bytecheftest");
        parameterMap.put(Authorization.PASSWORD, "bytecheftest");

        return MockParametersFactory.create(parameterMap);
    }

    private Parameters getReceieveEmailActionParameters(int port) {
        HashMap<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(EmailConstants.PORT, String.valueOf(port));
        parameterMap.put(EmailConstants.PROTOCOL, EmailProtocol.imap.name());

        return MockParametersFactory.create(parameterMap);
    }

    private Parameters getSendEmailActionParameters(int port) {
        HashMap<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(EmailConstants.PORT, String.valueOf(port));
        parameterMap.put("from", "test.from@test.com");
        parameterMap.put("to", List.of("bytecheftest@bytechef.io"));
        parameterMap.put("cc", List.of("test.cc@test.com"));
        parameterMap.put("bcc", List.of("test.bcc@test.com"));
        parameterMap.put("replyTo", List.of("test.replyto@test.com"));
        parameterMap.put("subject", "Do Test Until Success");
        parameterMap.put("content", "Magic appears always");

        return MockParametersFactory.create(parameterMap);
    }

}
