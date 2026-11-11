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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.email.constant.EmailConstants;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Ivica Cardic
 */
class SendEmailActionTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private static final byte[] CSV = "a,b\r\n1,2\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] XML = "<?xml version=\"1.0\"?><a>č</a>".getBytes(StandardCharsets.ISO_8859_1);

    @Test
    void testPerformSendsEachAttachmentAsItsOwnNamedPart() throws Exception {
        FileEntry csvFileEntry = mockFileEntry("report.csv", "text/csv");
        FileEntry xmlFileEntry = mockFileEntry("OSHO-01234567890-2026Q2-6-1.xml", "application/xml");

        ActionContext actionContext = mock(ActionContext.class);
        File file = mock(File.class);

        when(file.getInputStream(csvFileEntry)).thenAnswer(invocation -> new ByteArrayInputStream(CSV));
        when(file.getInputStream(xmlFileEntry)).thenAnswer(invocation -> new ByteArrayInputStream(XML));
        when(actionContext.file(any())).thenAnswer(invocation -> {
            ContextFunction<File, ?> function = invocation.getArgument(0);

            return function.apply(file);
        });

        SendEmailAction.perform(
            MockParametersFactory.create(
                Map.of(
                    EmailConstants.PORT, greenMail.getSmtp()
                        .getPort(),
                    "attachments", List.of(csvFileEntry, xmlFileEntry),
                    "content", "<p>Attached</p>",
                    "from", "sender@test.com",
                    "subject", "Files",
                    "to", List.of("recipient@test.com"))),
            MockParametersFactory.create(Map.of(EmailConstants.HOST, "localhost")),
            actionContext);

        greenMail.waitForIncomingEmail(1);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertEquals(1, receivedMessages.length);

        MimeMultipart mimeMultipart = (MimeMultipart) receivedMessages[0].getContent();

        assertEquals(3, mimeMultipart.getCount());

        assertAttachment((MimeBodyPart) mimeMultipart.getBodyPart(1), "report.csv", CSV);
        assertAttachment((MimeBodyPart) mimeMultipart.getBodyPart(2), "OSHO-01234567890-2026Q2-6-1.xml", XML);
    }

    private static void assertAttachment(MimeBodyPart mimeBodyPart, String fileName, byte[] content)
        throws Exception {

        assertEquals(Part.ATTACHMENT, mimeBodyPart.getDisposition());
        assertEquals(fileName, mimeBodyPart.getFileName());

        try (InputStream inputStream = mimeBodyPart.getInputStream()) {
            assertArrayEquals(content, inputStream.readAllBytes());
        }
    }

    private static FileEntry mockFileEntry(String name, String mimeType) {
        FileEntry fileEntry = mock(FileEntry.class);

        when(fileEntry.getMimeType()).thenReturn(mimeType);
        when(fileEntry.getName()).thenReturn(name);

        return fileEntry;
    }
}
