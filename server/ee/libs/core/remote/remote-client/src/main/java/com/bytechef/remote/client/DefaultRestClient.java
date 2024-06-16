/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.remote.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class DefaultRestClient extends AbstractRestClient {

    public DefaultRestClient() {
        super(RestClient.builder());
    }
}
