/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.web.rest.email;

import com.bytechef.platform.notification.email.NotificationEmailGateway;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Accepts proxied EMAIL-channel notification sends from apps without a mail stack (the distributed coordinator) and
 * delivers them through the local {@link NotificationEmailGateway} binding — on configuration-app that is
 * {@code MailService}, keeping SMTP credentials and templating in one place.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/notification-email-gateway")
public class RemoteNotificationEmailGatewayController {

    private final NotificationEmailGateway notificationEmailGateway;

    @SuppressFBWarnings("EI")
    public RemoteNotificationEmailGatewayController(NotificationEmailGateway notificationEmailGateway) {
        this.notificationEmailGateway = notificationEmailGateway;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/send-email",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> sendEmail(@RequestBody SendEmailRequest sendEmailRequest) {
        notificationEmailGateway.sendEmail(
            sendEmailRequest.to, sendEmailRequest.subject, sendEmailRequest.content, sendEmailRequest.html);

        return ResponseEntity.noContent()
            .build();
    }

    @SuppressFBWarnings("UUF_UNUSED")
    public record SendEmailRequest(String to, String subject, String content, boolean html) {
    }
}
