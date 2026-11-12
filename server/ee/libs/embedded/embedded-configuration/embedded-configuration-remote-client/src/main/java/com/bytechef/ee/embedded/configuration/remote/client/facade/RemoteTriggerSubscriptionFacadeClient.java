/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.dto.ComponentTriggerDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.ee.embedded.configuration.facade.TriggerSubscriptionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteTriggerSubscriptionFacadeClient implements TriggerSubscriptionFacade {

    @Override
    public TriggerSubscriptionDTO createTriggerSubscription(
        String externalUserId, String componentName, int componentVersion, String triggerName, Map<String, ?> input,
        @Nullable String name, @Nullable String externalId, @Nullable String webhookUrl, @Nullable Long instanceId,
        Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTriggerSubscription(String externalUserId, long id, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableTriggerSubscription(String externalUserId, long id, boolean enable, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ComponentTriggerDTO> getComponentTriggers(
        String externalUserId, String componentName, int componentVersion) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerSubscriptionDTO getTriggerSubscription(String externalUserId, long id, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TriggerSubscriptionDTO> getTriggerSubscriptions(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerSubscriptionDTO updateTriggerSubscription(
        String externalUserId, long id, Map<String, ?> input, @Nullable String name, @Nullable String externalId,
        @Nullable String webhookUrl, Environment environment) {

        throw new UnsupportedOperationException();
    }
}
