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

package com.bytechef.platform.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Mail;
import com.bytechef.platform.mail.config.MailIntTestConfiguration;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.User;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.activation.DataHandler;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration tests for {@link MailService}.
 */
@SpringBootTest(classes = MailIntTestConfiguration.class)
class MailServiceIntTest {

    private static final String[] languages = {
        "en",
    };
    private static final Pattern PATTERN_LOCALE_3 = Pattern.compile("([a-z]{2})-([a-zA-Z]{4})-([a-z]{2})");
    private static final Pattern PATTERN_LOCALE_2 = Pattern.compile("([a-z]{2})-([a-z]{2})");

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    @Autowired
    private MailService mailService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        doNothing().when(javaMailSender)
            .send(any(MimeMessage.class));
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void testSendEmail() throws Exception {
        mailService.sendEmail("john.doe@example.com", "testSubject", "testContent", false, false);

        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage message = messageCaptor.getValue();

        assertThat(message.getSubject()).isEqualTo("testSubject");
        assertThat(message.getAllRecipients()[0]).hasToString("john.doe@example.com");

        Mail mail = applicationProperties.getMail();

        assertThat(message.getFrom()[0]).hasToString(mail.getFrom());
        assertThat(message.getContent()).isInstanceOf(String.class);
        assertThat(message.getContent()).hasToString("testContent");

        DataHandler dataHandler = message.getDataHandler();

        assertThat(dataHandler.getContentType()).isEqualTo("text/plain; charset=UTF-8");
    }

    @Test
    void testSendHtmlEmail() throws Exception {
        mailService.sendEmail("john.doe@example.com", "testSubject", "testContent", false, true);

        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage message = messageCaptor.getValue();

        assertThat(message.getSubject()).isEqualTo("testSubject");
        assertThat(message.getAllRecipients()[0]).hasToString("john.doe@example.com");

        Mail mail = applicationProperties.getMail();

        assertThat(message.getFrom()[0]).hasToString(mail.getFrom());
        assertThat(message.getContent()).isInstanceOf(String.class);
        assertThat(message.getContent()).hasToString("testContent");

        DataHandler dataHandler = message.getDataHandler();

        assertThat(dataHandler.getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    void testSendMultipartEmail() throws Exception {
        mailService.sendEmail("john.doe@example.com", "testSubject", "testContent", true, false);

        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage message = messageCaptor.getValue();

        MimeMultipart mp = (MimeMultipart) message.getContent();

        BodyPart bodyPart = mp.getBodyPart(0);

        MimeBodyPart part = (MimeBodyPart) ((MimeMultipart) bodyPart.getContent()).getBodyPart(0);
        ByteArrayOutputStream aos = new ByteArrayOutputStream();

        part.writeTo(aos);

        assertThat(message.getSubject()).isEqualTo("testSubject");
        assertThat(message.getAllRecipients()[0]).hasToString("john.doe@example.com");

        Mail mail = applicationProperties.getMail();

        assertThat(message.getFrom()[0]).hasToString(mail.getFrom());
        assertThat(message.getContent()).isInstanceOf(Multipart.class);
        assertThat(aos).hasToString("\r\ntestContent");

        DataHandler dataHandler = part.getDataHandler();

        assertThat(dataHandler.getContentType()).isEqualTo("text/plain; charset=UTF-8");
    }

    @Test
    void testSendMultipartHtmlEmail() throws Exception {
        mailService.sendEmail("john.doe@example.com", "testSubject", "testContent", true, true);

        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage message = messageCaptor.getValue();

        MimeMultipart mp = (MimeMultipart) message.getContent();

        BodyPart bodyPart = mp.getBodyPart(0);

        MimeBodyPart part = (MimeBodyPart) ((MimeMultipart) bodyPart.getContent()).getBodyPart(0);
        ByteArrayOutputStream aos = new ByteArrayOutputStream();

        part.writeTo(aos);

        assertThat(message.getSubject()).isEqualTo("testSubject");
        assertThat(message.getAllRecipients()[0]).hasToString("john.doe@example.com");

        Mail mail = applicationProperties.getMail();

        assertThat(message.getFrom()[0]).hasToString(mail.getFrom());
        assertThat(message.getContent()).isInstanceOf(Multipart.class);
        assertThat(aos).hasToString("\r\ntestContent");

        DataHandler dataHandler = part.getDataHandler();

        assertThat(dataHandler.getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    void testSendEmailFromTemplate() throws Exception {
        User user = new User();

        user.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        user.setLogin("john");

        user.setEmail("john.doe@example.com");

        mailService.sendEmailFromTemplate(user, "mail/testEmail", "email.test.title");

        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage message = messageCaptor.getValue();

        assertThat(message.getSubject()).isEqualTo("test title");
        assertThat(message.getAllRecipients()[0]).hasToString(user.getEmail());

        Mail mail = applicationProperties.getMail();

        assertThat(message.getFrom()[0]).hasToString(mail.getFrom());

        Object content = message.getContent();

        assertThat(content.toString())
            .isEqualToNormalizingNewlines("<html>test title, http://127.0.0.1:8080, john</html>\n");

        DataHandler dataHandler = message.getDataHandler();

        assertThat(dataHandler.getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    void testSendActivationEmail() throws Exception {
        User user = new User();

        user.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        user.setLogin("john");
        user.setEmail("john.doe@example.com");

        mailService.sendActivationEmail(user);

        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage message = messageCaptor.getValue();

        assertThat(message.getAllRecipients()[0]).hasToString(user.getEmail());

        Mail mail = applicationProperties.getMail();

        assertThat(message.getFrom()[0]).hasToString(mail.getFrom());

        Object content = message.getContent();

        assertThat(content.toString()).isNotEmpty();

        DataHandler dataHandler = message.getDataHandler();

        assertThat(dataHandler.getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    void testCreationEmail() throws Exception {
        User user = new User();

        user.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        user.setLogin("john");
        user.setEmail("john.doe@example.com");

        mailService.sendCreationEmail(user);

        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage message = messageCaptor.getValue();

        assertThat(message.getAllRecipients()[0]).hasToString(user.getEmail());

        Mail mail = applicationProperties.getMail();

        assertThat(message.getFrom()[0]).hasToString(mail.getFrom());

        Object content = message.getContent();

        assertThat(content.toString()).isNotEmpty();

        DataHandler dataHandler = message.getDataHandler();

        assertThat(dataHandler.getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    void testSendPasswordResetMail() throws Exception {
        User user = new User();

        user.setLangKey(UserConstants.DEFAULT_LANGUAGE);
        user.setLogin("john");
        user.setEmail("john.doe@example.com");

        mailService.sendPasswordResetMail(user);

        verify(javaMailSender).send(messageCaptor.capture());

        MimeMessage message = messageCaptor.getValue();

        assertThat(message.getAllRecipients()[0]).hasToString(user.getEmail());

        Mail mail = applicationProperties.getMail();

        assertThat(message.getFrom()[0]).hasToString(mail.getFrom());

        Object content = message.getContent();

        assertThat(content.toString()).isNotEmpty();

        DataHandler dataHandler = message.getDataHandler();

        assertThat(dataHandler.getContentType()).isEqualTo("text/html;charset=UTF-8");
    }

    @Test
    void testSendEmailWithException() {
        doThrow(MailSendException.class).when(javaMailSender)
            .send(any(MimeMessage.class));

        try {
            mailService.sendEmail("john.doe@example.com", "testSubject", "testContent", false, false);
        } catch (Exception e) {
            fail("Exception shouldn't have been thrown");
        }
    }

    @Test
    @SuppressFBWarnings("OS")
    void testSendLocalizedEmailForAllSupportedLanguages() throws Exception {
        User user = new User();
        user.setLogin("john");
        user.setEmail("john.doe@example.com");

        for (String langKey : languages) {
            user.setLangKey(langKey);

            mailService.sendEmailFromTemplate(user, "mail/testEmail", "email.test.title");

            verify(javaMailSender, atLeastOnce()).send(messageCaptor.capture());

            MimeMessage message = messageCaptor.getValue();

            String propertyFilePath = "messages_" + getMessageSourceSuffixForLanguage(langKey) + ".properties";

            URL resource = this.getClass()
                .getClassLoader()
                .getResource(propertyFilePath);

            File file = new File(new URI(resource.getFile()).getPath());

            Properties properties = new Properties();

            properties.load(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

            String emailTitle = (String) properties.get("email.test.title");

            assertThat(message.getSubject()).isEqualTo(emailTitle);

            Object content = message.getContent();

            assertThat(content.toString())
                .isEqualToNormalizingNewlines("<html>" + emailTitle + ", http://127.0.0.1:8080, john</html>\n");
        }
    }

    /**
     * Convert a lang key to the Java locale.
     */
    private String getMessageSourceSuffixForLanguage(String langKey) {
        String javaLangKey = langKey;

        Matcher matcher2 = PATTERN_LOCALE_2.matcher(langKey);

        if (matcher2.matches()) {
            String group = matcher2.group(2);

            javaLangKey = matcher2.group(1) + "_" + group.toUpperCase();
        }

        Matcher matcher3 = PATTERN_LOCALE_3.matcher(langKey);

        if (matcher3.matches()) {
            String group = matcher3.group(3);

            javaLangKey = matcher3.group(1) + "_" + matcher3.group(2) + "_" + group.toUpperCase();
        }
        return javaLangKey;
    }
}
