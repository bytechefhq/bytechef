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
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * @author Ivica Cardic
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";
    private static final String BASE_URL = "baseUrl";
    private static final String WORKSPACE_NAME = "workspaceName";

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

    public boolean isMailConfigured() {
        return StringUtils.isNotBlank(mail.getHost());
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        this.sendEmailSync(to, subject, content, isMultipart, isHtml);
    }

    /**
     * Sends an email synchronously and lets failures propagate to the caller.
     *
     * @throws MailException         when the message cannot be built or sent
     * @throws IllegalStateException when the mail server is not configured
     */
    public void sendEmailNow(String to, String subject, String content, boolean isHtml) {
        if (StringUtils.isBlank(mail.getHost())) {
            throw new IllegalStateException("Mail server is not configured");
        }

        try {
            deliverEmail(to, subject, content, false, isHtml);
        } catch (MessagingException messagingException) {
            throw new MailPreparationException(messagingException);
        }
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

    /**
     * Mails an invited user the link on which they set their own password. Sent synchronously; a delivery failure is
     * thrown as a {@link MailException}.
     */
    public void sendCreationEmail(User user) {
        log.debug("Sending creation email to '{}'", user.getEmail());

        if (Objects.isNull(user.getEmail())) {
            throw new MailPreparationException("Email misses for user '" + user.getLogin() + "'");
        }

        deliverEmailFromTemplate(user, "mail/creationEmail", "email.invitation.title");
    }

    /**
     * Tells an existing account holder they were added to a workspace. That recipient has a password already, so the
     * invitation link would be both useless and alarming.
     */
    @Async
    public void sendWorkspaceMembershipEmail(User user, String workspaceName) {
        log.debug("Sending workspace membership email to '{}'", user.getEmail());

        if (Objects.isNull(user.getEmail())) {
            log.warn("Email misses for user '{}'", user.getLogin());

            return;
        }

        Locale locale = Locale.forLanguageTag(user.getLangKey());

        Context context = new Context(locale);

        context.setVariable(USER, user);
        context.setVariable(BASE_URL, mail.getBaseUrl());
        context.setVariable(WORKSPACE_NAME, workspaceName);

        String content = templateEngine.process("mail/workspaceMembershipEmail", context);
        String subject = messageSource.getMessage("email.workspace.membership.title", null, locale);

        this.sendEmailSync(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendPasswordResetMail(User user) {
        log.debug("Sending password reset email to '{}'", user.getEmail());

        this.sendEmailFromTemplateSync(user, "mail/passwordResetEmail", "email.reset.title");
    }

    private void sendEmailFromTemplateSync(User user, String templateName, String titleKey) {
        if (Objects.isNull(user.getEmail())) {
            log.warn("Email misses for user '{}'", user.getLogin());

            return;
        }

        try {
            deliverEmailFromTemplate(user, templateName, titleKey);
        } catch (MailException mailException) {
            log.error("Email could not be sent to user '{}'", user.getEmail(), mailException);
        }
    }

    private void deliverEmailFromTemplate(User user, String templateName, String titleKey) {
        Locale locale = Locale.forLanguageTag(user.getLangKey());

        Context context = new Context(locale);

        context.setVariable(USER, user);

        context.setVariable(BASE_URL, mail.getBaseUrl());

        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);

        try {
            deliverEmail(user.getEmail(), subject, content, false, true);
        } catch (MessagingException messagingException) {
            throw new MailPreparationException(messagingException);
        }
    }

    private void sendEmailSync(String recipient, String subject, String content, boolean isMultipart, boolean isHtml) {
        log.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}'",
            isMultipart, isHtml, recipient, subject);

        try {
            deliverEmail(recipient, subject, content, isMultipart, isHtml);
        } catch (MailException | MessagingException e) {
            log.error("Email could not be sent to user '{}'", recipient, e);
        }
    }

    private void deliverEmail(String recipient, String subject, String content, boolean isMultipart, boolean isHtml)
        throws MessagingException {

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());

        message.setTo(recipient);

        message.setFrom(mail.getFrom());

        message.setSubject(subject);
        message.setText(content, isHtml);

        javaMailSender.send(mimeMessage);

        log.debug("Sent email to User '{}'", recipient);
    }
}
