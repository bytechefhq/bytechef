/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

/**
 * Thrown by {@link TriggerSubscriptionDeliveryTaskHandler} for an event of a subscription that is not ENABLED yet while
 * a request holds its registration lease and is enabling it. The deliver task fails and the engine retries it, so the
 * event is delivered once the enable commits, or dropped when the enable ends without enabling the subscription. Its
 * stack trace is not filled in: the failure is expected and is logged with every retry.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TriggerSubscriptionEnablingException extends RuntimeException {

    public TriggerSubscriptionEnablingException(long subscriptionId) {
        super(
            "Trigger subscription id=%d is being enabled, the event is delivered once enabling completes".formatted(
                subscriptionId),
            null, false, false);
    }
}
