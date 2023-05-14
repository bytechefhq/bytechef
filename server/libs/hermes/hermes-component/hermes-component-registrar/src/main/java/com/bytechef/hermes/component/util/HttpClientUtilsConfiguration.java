package com.bytechef.hermes.component.util;

import org.springframework.beans.factory.InitializingBean;

/**
 * @author Ivica Cardic
 */
@org.springframework.context.annotation.Configuration
public class HttpClientUtilsConfiguration implements InitializingBean {

    private final HttpClientExecutor httpClientExecutor;

    public HttpClientUtilsConfiguration(HttpClientExecutor httpClientExecutor) {
        this.httpClientExecutor = httpClientExecutor;
    }

    @Override
    public void afterPropertiesSet() {
        HttpClientUtils.httpClientExecutor = httpClientExecutor;
    }
}
