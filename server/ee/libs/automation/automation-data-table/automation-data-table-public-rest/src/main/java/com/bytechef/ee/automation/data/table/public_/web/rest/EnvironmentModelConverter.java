/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import com.bytechef.ee.automation.data.table.public_.web.rest.model.EnvironmentModel;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component("automationDataTableEnvironmentModelConverter")
public class EnvironmentModelConverter implements Converter<String, EnvironmentModel> {

    @Override
    public @Nullable EnvironmentModel convert(String source) {
        String name = source.trim();

        if (name.isEmpty()) {
            return null;
        }

        return EnvironmentModel.valueOf(name.toUpperCase(Locale.ROOT));
    }
}
