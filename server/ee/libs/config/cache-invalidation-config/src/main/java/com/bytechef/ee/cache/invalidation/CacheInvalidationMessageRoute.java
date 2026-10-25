/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation;

import com.bytechef.message.route.MessageRoute;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum CacheInvalidationMessageRoute implements MessageRoute {

    CACHE_INVALIDATION_EVENTS(MessageRoute.Exchange.CONTROL, "cache.cache_invalidation_events");

    private MessageRoute.Exchange exchange;
    private String routeName;

    CacheInvalidationMessageRoute() {
    }

    CacheInvalidationMessageRoute(MessageRoute.Exchange exchange, String routeName) {
        this.exchange = exchange;
        this.routeName = routeName;
    }

    @Override
    public MessageRoute.Exchange getExchange() {
        return exchange;
    }

    @Override
    public String getName() {
        return routeName;
    }

    @Override
    public String toString() {
        return "CacheInvalidationMessageRoute{" +
            "exchange=" + exchange +
            ", routeName='" + routeName + '\'' +
            "} ";
    }
}
