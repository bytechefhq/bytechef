/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WebhookTriggerTestAdminFacade {

    String startWebhookTriggerTest(String workflowId, @Nullable String triggerName, long environmentId);

    void stopWebhookTriggerTest(String workflowId, @Nullable String triggerName, long environmentId);
}
