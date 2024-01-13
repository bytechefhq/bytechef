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

package com.bytechef.component.resend.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.resend.constant.ResendConstants.BCC;
import static com.bytechef.component.resend.constant.ResendConstants.CC;
import static com.bytechef.component.resend.constant.ResendConstants.FROM;
import static com.bytechef.component.resend.constant.ResendConstants.HEADERS;
import static com.bytechef.component.resend.constant.ResendConstants.HTML;
import static com.bytechef.component.resend.constant.ResendConstants.REPLY_TO;
import static com.bytechef.component.resend.constant.ResendConstants.SUBJECT;
import static com.bytechef.component.resend.constant.ResendConstants.TAGS;
import static com.bytechef.component.resend.constant.ResendConstants.TEXT;
import static com.bytechef.component.resend.constant.ResendConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.resend.util.ResendUtils;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.resend.services.emails.model.Tag;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class ResendSendEmailActionTest {

    private final ArgumentCaptor<CreateEmailOptions> createEmailOptionsArgumentCaptor =
        ArgumentCaptor.forClass(CreateEmailOptions.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final CreateEmailResponse mockedCreateEmailResponse = mock(CreateEmailResponse.class);
    private final Emails mockedEmails = mock(Emails.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testPerform() throws ResendException {
        List<String> toList = List.of("to@mail.com");
        List<String> bccList = List.of("bcc@mail.com");
        List<String> ccList = List.of("cc@mail.com");
        List<String> replyToList = List.of("reply@mail.com");
        List<Tag> tags = List.of(Tag.builder()
            .name("category")
            .value("confirm_email")
            .build());
        Map<String, String> headerMap = Map.of("X-Entity-Ref-ID", "123456789");
        List<Attachment> attachments = List.of(new Attachment.Builder().fileName("fileName")
            .build());

        when(mockedParameters.get(TOKEN))
            .thenReturn("token");
        when(mockedParameters.getRequiredString(FROM))
            .thenReturn("from@mail.com");
        when(mockedParameters.getList(TO, String.class, List.of()))
            .thenReturn(toList);
        when(mockedParameters.getString(SUBJECT))
            .thenReturn("subject");
        when(mockedParameters.getList(BCC, String.class, List.of()))
            .thenReturn(bccList);
        when(mockedParameters.getList(CC, String.class, List.of()))
            .thenReturn(ccList);
        when(mockedParameters.getList(REPLY_TO, String.class, List.of()))
            .thenReturn(replyToList);
        when(mockedParameters.getString(HTML))
            .thenReturn("html");
        when(mockedParameters.getString(TEXT))
            .thenReturn("text");
        when(mockedParameters.getMap(HEADERS, String.class, Map.of()))
            .thenReturn(headerMap);
        when(mockedParameters.getList(TAGS, Tag.class, List.of()))
            .thenReturn(tags);

        try (MockedConstruction<Resend> resendMockedConstruction = mockConstruction(
            Resend.class,
            (mock, context) -> {
                when(mock.emails()).thenReturn(mockedEmails);
                when(mockedEmails.send(createEmailOptionsArgumentCaptor.capture()))
                    .thenReturn(mockedCreateEmailResponse);
            })) {

            try (MockedStatic<ResendUtils> resendUtilsMockedStatic = mockStatic(ResendUtils.class)) {
                resendUtilsMockedStatic.when(
                    () -> ResendUtils.getAttachments(mockedParameters, mockedContext))
                    .thenReturn(attachments);

                CreateEmailResponse result =
                    ResendSendEmailAction.perform(mockedParameters, mockedParameters, mockedContext);

                List<Resend> resends = resendMockedConstruction.constructed();

                assertEquals(1, resends.size());
                assertEquals(mockedCreateEmailResponse, result);

                Resend resend = resends.getFirst();

                verify(resend, times(1)).emails();
                verify(resend.emails(), times(1)).send(createEmailOptionsArgumentCaptor.capture());

                CreateEmailOptions sendEmailRequest = createEmailOptionsArgumentCaptor.getValue();

                assertEquals("from@mail.com", sendEmailRequest.getFrom());
                assertEquals(toList, sendEmailRequest.getTo());
                assertEquals("subject", sendEmailRequest.getSubject());
                assertEquals(bccList, sendEmailRequest.getBcc());
                assertEquals(ccList, sendEmailRequest.getCc());
//            assertEquals(replyToList, sendEmailRequest.getReplyTo());
                assertEquals("html", sendEmailRequest.getHtml());
                assertEquals("text", sendEmailRequest.getText());
                assertEquals(attachments, sendEmailRequest.getAttachments());
                assertEquals(headerMap, sendEmailRequest.getHeaders());
                assertEquals(tags, sendEmailRequest.getTags());
            }
        }
    }
}
