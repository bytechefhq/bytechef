/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.web.rest.service;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.service.NotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the notification read path to the other apps' remote clients — the coordinator's job-status notification
 * listener resolves the delivery targets for an event type through here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/notification-service")
public class RemoteNotificationServiceController {

    private final NotificationService notificationService;

    @SuppressFBWarnings("EI")
    public RemoteNotificationServiceController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-notifications",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Notification>> getNotifications() {
        return ResponseEntity.ok(notificationService.getNotifications());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-notifications-by-event-type/{eventType}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable NotificationEvent.Type eventType) {
        return ResponseEntity.ok(notificationService.getNotifications(eventType));
    }
}
