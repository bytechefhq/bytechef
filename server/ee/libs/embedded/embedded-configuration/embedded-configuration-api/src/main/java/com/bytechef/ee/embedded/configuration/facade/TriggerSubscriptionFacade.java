/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ComponentTriggerDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface TriggerSubscriptionFacade {

    TriggerSubscriptionDTO createTriggerSubscription(
        String externalUserId, String componentName, int componentVersion, String triggerName, Map<String, ?> input,
        @Nullable String name, @Nullable String externalId, @Nullable String webhookUrl, @Nullable Long instanceId,
        Environment environment);

    void deleteTriggerSubscription(String externalUserId, long id, Environment environment);

    void enableTriggerSubscription(String externalUserId, long id, boolean enable, Environment environment);

    List<ComponentTriggerDTO> getComponentTriggers(String externalUserId, String componentName, int componentVersion);

    TriggerSubscriptionDTO getTriggerSubscription(String externalUserId, long id, Environment environment);

    List<TriggerSubscriptionDTO> getTriggerSubscriptions(String externalUserId, Environment environment);

    TriggerSubscriptionDTO updateTriggerSubscription(
        String externalUserId, long id, Map<String, ?> input, @Nullable String name, @Nullable String externalId,
        @Nullable String webhookUrl, Environment environment);
}
