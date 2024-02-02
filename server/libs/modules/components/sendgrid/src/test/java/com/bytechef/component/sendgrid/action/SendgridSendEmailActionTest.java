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

package com.bytechef.component.sendgrid.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.BCC;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.CC;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.CONTENT_VALUE;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.DYNAMIC_TEMPLATE;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.FROM;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.REPLY_TO;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SUBJECT;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.TEMPLATE_ID;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.sendgrid.util.SendgridUtils;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.objects.Attachments;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * @author Marko Krišković
 */

public class SendgridSendEmailActionTest extends AbstractSendgridActionTest {
    private final ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
    private final Response mockedResponse = mock(Response.class);

    @Test
    public void testPerform() throws IOException {
        when(mockedParameters.get(TOKEN))
            .thenReturn("token");
        when(mockedParameters.getRequiredString(FROM))
            .thenReturn("from@mail.com");
        when(mockedParameters.getList(TO, String.class, List.of()))
            .thenReturn(List.of("to@mail.com"));
        when(mockedParameters.getString(REPLY_TO))
            .thenReturn("replyTo@mail.com");
        when(mockedParameters.getList(CC, String.class, List.of()))
            .thenReturn(List.of("cc@mail.com"));
        when(mockedParameters.getList(BCC, String.class, List.of()))
            .thenReturn(List.of("bcc@mail.com"));
        when(mockedParameters.getRequiredString(SUBJECT))
            .thenReturn("Sending with SendGrid is Fun");
        when(mockedParameters.getRequiredString(CONTENT_VALUE))
            .thenReturn("and easy to do anywhere, even with Java");
        when(mockedParameters.getString(TEMPLATE_ID))
            .thenReturn("template_id");
        when(mockedParameters.getMap(DYNAMIC_TEMPLATE, Object.class, Map.of()))
            .thenReturn(Map.of("key1", "value1", "key2", "value2"));

        try (MockedStatic<SendgridUtils> sendgridUtilsMockedStatic = mockStatic(SendgridUtils.class)) {
            sendgridUtilsMockedStatic
                .when(() -> SendgridUtils.getAttachments(mockedParameters, mockedContext))
                .thenReturn(List.of(new Attachments.Builder("fileName", "fileContent").build()));


            try (MockedConstruction<SendGrid> sendgridMockedConstruction = mockConstruction(SendGrid.class,
                (mock, context) -> when(mock.api(requestArgumentCaptor.capture())).thenReturn(mockedResponse))){

                Response response = SendgridSendEmailAction.perform(mockedParameters, mockedParameters, mockedContext);

                assertNotNull(response);

                List<SendGrid> requests = sendgridMockedConstruction.constructed();
                SendGrid mockSendgrid = requests.getFirst();

                verify(mockSendgrid, times(1)).api(requestArgumentCaptor.capture());

                Request request = requestArgumentCaptor.getValue();

                String expected = "{\"from\":{\"email\":\"from@mail.com\"},\"subject\":\"Sending with SendGrid is Fun\",\"personalizations\":[{\"to\":[{\"email\":\"to@mail.com\"}],\"cc\":[{\"email\":\"cc@mail.com\"}],\"bcc\":[{\"email\":\"bcc@mail.com\"}],\"dynamic_template_data\":{\"key1\":\"value1\",\"key2\":\"value2\"}}],\"content\":[{\"type\":\"text/plain\",\"value\":\"and easy to do anywhere, even with Java\"}],\"attachments\":[{\"content\":\"fileContent\",\"filename\":\"fileName\"}],\"template_id\":\"template_id\",\"reply_to\":{\"email\":\"replyTo@mail.com\"}}";
                assertEquals(Method.POST, request.getMethod());
                assertEquals("mail/send", request.getEndpoint());
                assertEquals(expected, request.getBody());
            }
        }
    }
}
