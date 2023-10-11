
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.commons.webclient;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Ivica Cardic
 */
@Component
public class LoadBalancedWebClient extends AbstractWebClient {

    public LoadBalancedWebClient(WebClient.Builder loadBalancedWebClientBuilder) {
        super(loadBalancedWebClientBuilder);
    }
}
