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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Mail;
import com.bytechef.platform.user.domain.User;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Service for sending emails asynchronously.
 * <p>
 * We use the {@link Async} annotation to send emails asynchronously.
 *
 * @author Ivica Cardic
 */
@Service
public class MailService {

    private final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";
    private static final String BASE_URL = "baseUrl";
    private static final String PASSWORD = "password";

    private final JavaMailSender javaMailSender;
    private final Mail mail;
    private final MessageSource messageSource;
    private final SpringTemplateEngine templateEngine;

    @SuppressFBWarnings("EI")
    public MailService(
        JavaMailSender javaMailSender, ApplicationProperties applicationProperties, MessageSource messageSource,
        SpringTemplateEngine templateEngine) {

        this.javaMailSender = javaMailSender;
        this.mail = applicationProperties.getMail();
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;

        if (StringUtils.isBlank(mail.getHost()) && log.isWarnEnabled()) {
            log.warn("Mail server is not configured, not sending mails");
        }
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        this.sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    @Async
    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        this.sendEmailFromTemplateSync(user, templateName, titleKey);
    }

    @Async
    public void sendActivationEmail(User user) {
        log.debug("Sending activation email to '{}'", user.getEmail());

        this.sendEmailFromTemplateSync(user, "mail/activationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        log.debug("Sending creation email to '{}'", user.getEmail());

        this.sendEmailFromTemplateSync(user, "mail/creationEmail", "email.activation.title");
    }

    @Async
    public void sendInvitationEmail(User user, String password) {
        log.debug("Sending invitation email to '{}'", user.getEmail());

        this.sendInvitationEmailSync(user, password, "mail/invitationEmail", "email.invitation.title");
    }

    @Async
    public void sendPasswordResetMail(User user) {
        log.debug("Sending password reset email to '{}'", user.getEmail());

        this.sendEmailFromTemplateSync(user, "mail/passwordResetEmail", "email.reset.title");
    }

    private void sendEmailFromTemplateSync(User user, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            log.debug("Email doesn't exist for user '{}'", user.getLogin());

            return;
        }

        Locale locale = Locale.forLanguageTag(user.getLangKey());

        Context context = new Context(locale);

        context.setVariable(USER, user);

        context.setVariable(BASE_URL, mail.getBaseUrl());

        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);

        this.sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    private void sendInvitationEmailSync(User user, String password, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            log.debug("Email doesn't exist for user '{}'", user.getLogin());

            return;
        }

        Locale locale = Locale.forLanguageTag(user.getLangKey());

        Context context = new Context(locale);

        context.setVariable(USER, user);
        context.setVariable(BASE_URL, mail.getBaseUrl());
        context.setVariable(PASSWORD, password);

        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);

        this.sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    private void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        log.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            isMultipart, isHtml, to, subject, content);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());

            message.setTo(to);

            message.setFrom(mail.getFrom());

            message.setSubject(subject);
            message.setText(content, isHtml);

            javaMailSender.send(mimeMessage);

            log.debug("Sent email to User '{}'", to);
        } catch (MailException | MessagingException e) {
            log.error("Email could not be sent to user '{}'", to, e);
        }
    }
}
