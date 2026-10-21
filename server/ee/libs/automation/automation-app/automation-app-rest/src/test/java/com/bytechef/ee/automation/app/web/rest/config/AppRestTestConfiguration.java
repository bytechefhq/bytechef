/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.rest.config;

import com.bytechef.ee.automation.app.web.rest.AppIconApiController;
import com.bytechef.ee.automation.app.web.rest.AppPortalApiController;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.web.rest.error.GlobalResponseEntityExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 */
@Configuration
@Import({
    AppIconApiController.class, AppPortalApiController.class, GlobalResponseEntityExceptionHandler.class,
    JacksonConfiguration.class
})
public class AppRestTestConfiguration {
}
