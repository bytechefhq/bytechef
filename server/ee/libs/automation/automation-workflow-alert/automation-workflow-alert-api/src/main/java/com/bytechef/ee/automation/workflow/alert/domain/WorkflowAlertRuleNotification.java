/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.domain;

import java.util.Objects;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Join row binding an alert rule to a platform {@code Notification} delivery target. The notification id is a plain
 * column (not an aggregate reference) because {@code Notification} lives in a different module/aggregate —
 * platform-notification remains the owner of channel configuration.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("workflow_alert_rule_notification")
public class WorkflowAlertRuleNotification {

    @Column("notification_id")
    private Long notificationId;

    public WorkflowAlertRuleNotification(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof WorkflowAlertRuleNotification that)) {
            return false;
        }

        return Objects.equals(notificationId, that.notificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId);
    }
}
