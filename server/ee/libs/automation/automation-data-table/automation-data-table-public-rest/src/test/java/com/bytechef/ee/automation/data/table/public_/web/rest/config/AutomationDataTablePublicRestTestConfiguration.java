/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest.config;

import com.bytechef.ee.automation.data.table.public_.web.rest.DataTableApiController;
import com.bytechef.ee.automation.data.table.public_.web.rest.DataTableApiSupport;
import com.bytechef.ee.automation.data.table.public_.web.rest.EnvironmentModelConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@Import({
    DataTableApiController.class, DataTableApiSupport.class, EnvironmentModelConverter.class, JacksonConfiguration.class
})
public class AutomationDataTablePublicRestTestConfiguration {
}
