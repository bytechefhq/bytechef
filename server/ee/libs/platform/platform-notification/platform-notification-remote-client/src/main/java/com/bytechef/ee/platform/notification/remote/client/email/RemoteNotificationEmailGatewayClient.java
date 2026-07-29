/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.client.email;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.notification.email.NotificationEmailGateway;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Proxies EMAIL-channel notification sends to configuration-app, where {@code MailService} and the SMTP credentials
 * live — the distributed coordinator delivers email notifications through this client instead of carrying a mail stack
 * of its own.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteNotificationEmailGatewayClient implements NotificationEmailGateway {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String NOTIFICATION_EMAIL_GATEWAY = "/remote/notification-email-gateway";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteNotificationEmailGatewayClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public void sendEmail(String to, String subject, String content, boolean html) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(NOTIFICATION_EMAIL_GATEWAY + "/send-email")
                .build(),
            new SendEmailRequest(to, subject, content, html));
    }

    private record SendEmailRequest(String to, String subject, String content, boolean html) {
    }
}
